## SDK Integration

The rate limiter ships as a Java SDK — a single class that wraps the
HTTP call. Any Java service can integrate in minutes.

### Prerequisites

Currently the SDK is published to local Maven. Run the following in
the rate limiter project root first:

```bash
./mvnw clean install -DskipTests
```

This installs the JAR to your local `.m2` repository making it available
as a Maven dependency on your machine.

> For team or public use the SDK should be published to GitHub Packages
> or Maven Central.

---

### 1. Add the dependency

```xml
<dependency>
    <groupId>com.ratelimiter</groupId>
    <artifactId>rl-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

### 2. Register the client as a Spring bean

```java
@Configuration
public class RateLimiterConfig {

    @Bean
    public RateLimiterClient rateLimiterClient() {
        return new RateLimiterClient("http://localhost:8082");
    }
}
```

---

### 3. Create a filter

```java
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterClient rateLimiterClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = extractJwt(request);
        RateLimiterClient.Result result = rateLimiterClient.check("your-service", jwt);

        if (!result.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\": \"Too Many Requests\", " +
                "\"retryAfterSeconds\": " + result.retryAfterSeconds() + "}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

---

### 4. Register the filter in Spring Security

```java
http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
```

---

### 5. Add a rule for your service

```bash
curl -X POST http://localhost:8082/admin/rules \
  -H "Content-Type: application/json" \
  -d '{"serviceId": "your-service", "capacity": 20, "refillRate": 5}'
```

---

### Fail open behaviour

If the rate limiter service is unreachable the SDK automatically fails
open — the request is allowed through. Your service never goes down
because of a rate limiter outage.

---

### Result fields

| Field | Type | Description |
|---|---|---|
| `allowed` | boolean | Whether the request should proceed |
| `remaining` | int | Tokens left in the bucket |
| `resetAt` | long | Unix timestamp when bucket will be full |
| `retryAfterSeconds` | int | Seconds to wait before retrying (0 if allowed) |