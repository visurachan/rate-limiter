package com.ratelimiter.rl_service.core;

import com.ratelimiter.rl_service.model.RateLimitEvent;
import com.ratelimiter.rl_service.model.RateLimitResult;
import com.ratelimiter.rl_service.model.RateLimitRule;
import com.ratelimiter.rl_service.repository.RateLimitEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final ConfigLoader configLoader;
    private final KeyResolver keyResolver;
    private final TokenBucketStrategy tokenBucketStrategy;
    private final RateLimitEventRepository eventRepository;

    public RateLimitResult check(String serviceId, HttpServletRequest request) {


        RateLimitRule rule = configLoader.loadRule(serviceId);


        String redisKey = keyResolver.resolve(request, serviceId);
        String userId = extractUserId(redisKey);


        List<Long> luaResult = tokenBucketStrategy.consume(
                redisKey,
                rule.getCapacity(),
                rule.getRefillRate()
        );


        RateLimitResult result = buildResult(luaResult, rule);


        persistEventAsync(serviceId, userId, result);

        log.debug("Check — serviceId={} userId={} allowed={} remaining={}",
                serviceId, userId, result.isAllowed(), result.getRemaining());

        return result;
    }

    private RateLimitResult buildResult(List<Long> luaResult, RateLimitRule rule) {
        if (luaResult == null || luaResult.isEmpty()) {
            log.warn("Null result from Redis — failing open");
            return RateLimitResult.failOpen();
        }

        long allowed   = luaResult.get(0);
        long remaining = luaResult.get(1);
        long resetAt   = calculateResetAt(remaining, rule);

        if (allowed == 1L) {
            return RateLimitResult.allow((int) remaining, resetAt);
        } else {
            int retryAfter = calculateRetryAfter(rule);
            return RateLimitResult.block(resetAt, retryAfter);
        }
    }

    private long calculateResetAt(long remaining, RateLimitRule rule) {
        int tokensNeeded = rule.getCapacity() - (int) remaining;
        if (tokensNeeded <= 0) return System.currentTimeMillis() / 1000;
        long secondsToFull = (long) Math.ceil((double) tokensNeeded / rule.getRefillRate());
        return System.currentTimeMillis() / 1000 + secondsToFull;
    }

    private int calculateRetryAfter(RateLimitRule rule) {
        return (int) Math.ceil(1.0 / rule.getRefillRate());
    }

    private String extractUserId(String redisKey) {
        String[] parts = redisKey.split(":", 3);
        return parts.length == 3 ? parts[2] : redisKey;
    }

    @Async
    public void persistEventAsync(String serviceId, String userId, RateLimitResult result) {
        try {
            RateLimitEvent event = RateLimitEvent.builder()
                    .serviceId(serviceId)
                    .userId(userId)
                    .allowed(result.isAllowed())
                    .remaining(result.getRemaining())
                    .build();
            eventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to persist audit event: {}", e.getMessage());
        }
    }
}