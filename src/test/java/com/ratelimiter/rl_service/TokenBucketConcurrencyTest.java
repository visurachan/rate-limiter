package com.ratelimiter.rl_service;

import com.ratelimiter.rl_service.core.TokenBucketStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketConcurrencyTest {

    private TokenBucketStrategy tokenBucketStrategy;
    private StringRedisTemplate redisTemplate;
    private LettuceConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6381);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        redisTemplate.delete("rl:banking:test-concurrency-user");
        tokenBucketStrategy = new TokenBucketStrategy(redisTemplate);


    }

    @Test
    void twentyFiveThreadsExactlyTwentyAllowed() throws InterruptedException {
        int totalThreads = 25;
        int limit        = 20;
        String redisKey  = "rl:banking:test-concurrency-user" ;

        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger blocked = new AtomicInteger(0);

        // Latch makes all 25 threads start at exactly the same moment
        CountDownLatch startGun   = new CountDownLatch(1);
        CountDownLatch allDone    = new CountDownLatch(totalThreads);

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);

        for (int i = 0; i < totalThreads; i++) {

            executor.submit(() -> {
                try {
                    startGun.await(); // all threads wait here


                    List<Long> result = tokenBucketStrategy.consume(redisKey, limit, 5);
                    if (result.get(0) == 1L) {
                        allowed.incrementAndGet();
                    } else {
                        blocked.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    blocked.incrementAndGet();
                } finally {
                    allDone.countDown();
                }
            });
        }

        startGun.countDown(); // fire — all 25 threads go at once
        allDone.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("Allowed: " + allowed.get());
        System.out.println("Blocked: " + blocked.get());
        System.out.println("Total:   " + (allowed.get() + blocked.get()));

        assertThat(allowed.get() + blocked.get()).isEqualTo(25); // check first
        assertThat(allowed.get()).isEqualTo(20);
        assertThat(blocked.get()).isEqualTo(5);


    }
}