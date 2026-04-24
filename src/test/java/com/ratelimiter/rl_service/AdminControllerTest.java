package com.ratelimiter.rl_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listRules_returnsBankingRule() throws Exception {
        mockMvc.perform(get("/admin/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceId").value("banking"))
                .andExpect(jsonPath("$[0].capacity").isNumber())
                .andExpect(jsonPath("$[0].refillRate").isNumber())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void upsertRule_updatesCapacity() throws Exception {

        mockMvc.perform(post("/admin/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\": \"banking\", \"capacity\": 50, \"refillRate\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").value("banking"))
                .andExpect(jsonPath("$.capacity").value(50))
                .andExpect(jsonPath("$.refillRate").value(10));


        mockMvc.perform(get("/admin/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].capacity").value(50));


        mockMvc.perform(post("/admin/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\": \"banking\", \"capacity\": 20, \"refillRate\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(20));
    }
}
