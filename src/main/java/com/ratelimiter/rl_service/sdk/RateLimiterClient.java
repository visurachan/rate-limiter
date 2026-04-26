package com.ratelimiter.rl_service.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * SDK client for the Rate Limiter Service.
 *
 * Usage:
 *   RateLimiterClient client = new RateLimiterClient("http://localhost:8081");
 *   RateLimiterClient.Result result = client.check("banking", jwtToken);
 *   if (!result.allowed()) { return 429; }
 */
@Slf4j
public class RateLimiterClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RateLimiterClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Checks whether the request should be allowed or blocked.
     *
     * @param serviceId  the service identifier e.g. "banking"
     * @param jwtToken   the Bearer token from the incoming request (can be null)
     * @return           Result with allowed, remaining, resetAt, retryAfterSeconds
     */
    public Result check(String serviceId, String jwtToken) {
        try {
            String body = "{\"serviceId\": \"" + serviceId + "\"}";

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/rate-limit/check"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(2));

            // attach JWT if present
            if (jwtToken != null && !jwtToken.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + jwtToken);
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode json = objectMapper.readTree(response.body());

            return new Result(
                    json.get("allowed").asBoolean(),
                    json.get("remaining").asInt(),
                    json.get("resetAt").asLong(),
                    json.has("retryAfterSeconds") && !json.get("retryAfterSeconds").isNull()
                            ? json.get("retryAfterSeconds").asInt() : 0
            );

        } catch (Exception e) {
            // if rate limiter is unreachable — fail open
            log.warn("Rate limiter unreachable — failing open. Error: {}", e.getMessage());
            return Result.failOpen();
        }
    }



    public record Result(
            boolean allowed,
            int remaining,
            long resetAt,
            int retryAfterSeconds
    ) {
        public static Result failOpen() {
            return new Result(true, -1, 0, 0);
        }
    }
}