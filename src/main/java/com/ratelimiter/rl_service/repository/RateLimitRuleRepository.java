package com.ratelimiter.rl_service.repository;

import com.ratelimiter.rl_service.model.RateLimitRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RateLimitRuleRepository extends JpaRepository<RateLimitRule, UUID> {

    Optional<RateLimitRule> findByServiceIdAndActiveTrue(String serviceId);
}