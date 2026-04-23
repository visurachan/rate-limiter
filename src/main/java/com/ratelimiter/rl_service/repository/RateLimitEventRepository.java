package com.ratelimiter.rl_service.repository;

import com.ratelimiter.rl_service.model.RateLimitEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RateLimitEventRepository extends JpaRepository<RateLimitEvent, UUID> {
}