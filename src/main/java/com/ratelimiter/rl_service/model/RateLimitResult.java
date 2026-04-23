package com.ratelimiter.rl_service.model;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RateLimitResult {

    boolean allowed;
    int remaining;
    long resetAt;
    int retryAfterSeconds;

    public static RateLimitResult allow(int remaining, long resetAt) {
        return RateLimitResult.builder()
                .allowed(true)
                .remaining(remaining)
                .resetAt(resetAt)
                .retryAfterSeconds(0)
                .build();
    }

    public static RateLimitResult block(long resetAt, int retryAfterSeconds) {
        return RateLimitResult.builder()
                .allowed(false)
                .remaining(0)
                .resetAt(resetAt)
                .retryAfterSeconds(retryAfterSeconds)
                .build();
    }

    public static RateLimitResult failOpen() {
        long resetAt = System.currentTimeMillis() / 1000 + 60;
        return RateLimitResult.builder()
                .allowed(true)
                .remaining(-1)
                .resetAt(resetAt)
                .retryAfterSeconds(0)
                .build();
    }
}