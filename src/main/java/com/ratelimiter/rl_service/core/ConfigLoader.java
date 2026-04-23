package com.ratelimiter.rl_service.core;

import com.ratelimiter.rl_service.model.RateLimitRule;
import com.ratelimiter.rl_service.repository.RateLimitRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigLoader {

    private final RateLimitRuleRepository ruleRepository;

    @Cacheable(value = "rateLimitRules", key = "#serviceId")
    public RateLimitRule loadRule(String serviceId) {
        log.debug("Cache miss — loading from DB for serviceId={}", serviceId);
        return ruleRepository.findByServiceIdAndActiveTrue(serviceId)
                .orElseThrow(() -> new RuleNotFoundException(
                        "No active rule found for serviceId: " + serviceId));
    }

    @CacheEvict(value = "rateLimitRules", key = "#serviceId")
    public void evictCache(String serviceId) {
        log.info("Cache evicted for serviceId={}", serviceId);
    }

    public static class RuleNotFoundException extends RuntimeException {
        public RuleNotFoundException(String message) {
            super(message);
        }
    }
}
