package com.ratelimiter.rl_service.core;



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
}
