package com.ratelimiter.rl_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rate_limit_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private String serviceId;

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "refill_rate", nullable = false)
    private int refillRate;

    @Column(name = "active")
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at",updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


}
