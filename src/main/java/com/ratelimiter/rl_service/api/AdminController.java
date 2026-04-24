package com.ratelimiter.rl_service.api;

import com.ratelimiter.rl_service.core.ConfigLoader;
import com.ratelimiter.rl_service.model.RateLimitRule;
import com.ratelimiter.rl_service.repository.RateLimitRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/rules")
@RequiredArgsConstructor
public class AdminController {

    private final RateLimitRuleRepository ruleRepository;
    private final ConfigLoader configLoader;

    @GetMapping
    public ResponseEntity<List<RateLimitRule>> listRules() {
        return ResponseEntity.ok(ruleRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<RateLimitRule> upsertRule(@RequestBody UpsertRuleRequest req) {
        RateLimitRule rule = ruleRepository
                .findByServiceIdAndActiveTrue(req.serviceId())
                .orElse(RateLimitRule.builder()
                        .serviceId(req.serviceId())
                        .active(true)
                        .build());

        rule.setCapacity(req.capacity());
        rule.setRefillRate(req.refillRate());

        RateLimitRule saved = ruleRepository.save(rule);
        configLoader.evictCache(req.serviceId());

        log.info("Rule updated — serviceId={} capacity={} refillRate={}",
                req.serviceId(), req.capacity(), req.refillRate());

        return ResponseEntity.ok(saved);
    }

    public record UpsertRuleRequest(
            String serviceId,
            int capacity,
            int refillRate
    ) {}
}