package com.ratelimiter.rl_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void checkEndpoint_noJwt_returnsAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/rate-limit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\": \"banking\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    void checkEndpoint_exhaustBucket_returns429() throws Exception {
        // drain the bucket
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/rate-limit/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"serviceId\": \"banking\"}"));
        }

        // next request should be blocked
        mockMvc.perform(post("/api/v1/rate-limit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\": \"banking\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void pingEndpoint_returns200() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/rate-limit/ping"))
                .andExpect(status().isOk());
    }
}