package com.ratelimiter.rl_service.core;



import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class TokenBucketStrategy {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> tokenBucketScript;

    public TokenBucketStrategy(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = buildScript();
    }

    @CircuitBreaker(name = "redis", fallbackMethod = "failOpen")
    public List<Long> consume(String redisKey, int capacity, int refillRate) {
        long nowMs = System.currentTimeMillis();

        List result = redisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(redisKey),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(nowMs)
        );

        log.debug("Lua result for key={} → {}", redisKey, result);
        return result;
    }

    private DefaultRedisScript<List> buildScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(
                new ResourceScriptSource(
                        new ClassPathResource("scripts/token_bucket.lua")
                )
        );
        script.setResultType(List.class);
        return script;
    }

    public List<Long> failOpen(String redisKey, int capacity, int refillRate, Throwable ex) {
        log.warn("CIRCUIT OPEN — failing open for key={}. Redis error: {}", redisKey, ex.getMessage());
        return List.of(1L, -1L);  // allowed=true, remaining=-1 (unknown)
    }
}
