package com.ratelimiter.rl_service.api;

import com.ratelimiter.rl_service.core.RateLimitService;
import com.ratelimiter.rl_service.model.RateLimitResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/rate-limit")
@RequiredArgsConstructor
public class RateLimitController {

    private final RateLimitService rateLimitService;

    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> check(
            @RequestBody CheckRequest body,
            HttpServletRequest request) {

        RateLimitResult result = rateLimitService.check(body.serviceId(), request);

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("allowed",           result.isAllowed());
        responseBody.put("remaining",         result.getRemaining());
        responseBody.put("resetAt",           result.getResetAt());
        responseBody.put("retryAfterSeconds", result.isAllowed() ? null : result.getRetryAfterSeconds());

        HttpStatus status = result.isAllowed()
                ? HttpStatus.OK
                : HttpStatus.TOO_MANY_REQUESTS;

        return ResponseEntity
                .status(status)
                .header("X-RateLimit-Remaining", String.valueOf(result.getRemaining()))
                .header("X-RateLimit-Reset",     String.valueOf(result.getResetAt()))
                .header("Retry-After",           result.isAllowed() ? null
                        : String.valueOf(result.getRetryAfterSeconds()))
                .body(responseBody);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Rate Limiter Service OK");
    }

    public record CheckRequest(String serviceId) {}
}