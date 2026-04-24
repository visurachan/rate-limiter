package com.ratelimiter.rl_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitMetrics {

    private final MeterRegistry meterRegistry;
    private final Timer redisTimer;

    // cache counters per serviceId to avoid recreating them
    private final ConcurrentHashMap<String, Counter> allowedCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> blockedCounters = new ConcurrentHashMap<>();

    public RateLimitMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.redisTimer = Timer.builder("rate_limit_redis_duration_ms")
                .description("Lua script execution time in Redis")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void recordAllowed(String serviceId) {
        allowedCounters.computeIfAbsent(serviceId, id ->
                Counter.builder("rate_limit_allowed_total")
                        .description("Total allowed requests")
                        .tag("serviceId", id)
                        .register(meterRegistry)
        ).increment();
    }

    public void recordBlocked(String serviceId) {
        blockedCounters.computeIfAbsent(serviceId, id ->
                Counter.builder("rate_limit_blocked_total")
                        .description("Total blocked requests")
                        .tag("serviceId", id)
                        .register(meterRegistry)
        ).increment();
    }

    public void recordRedisDuration(long durationMs) {
        redisTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}