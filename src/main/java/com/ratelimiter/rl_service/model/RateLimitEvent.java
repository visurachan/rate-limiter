package com.ratelimiter.rl_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rate_limit_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_id")
    private String serviceId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "allowed")
    private Boolean allowed;

    @Column(name = "remaining")
    private Integer remaining;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;


}