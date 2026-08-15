package com.accesszero.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetVerificationForUser() throws Exception {
        mockMvc.perform(get("/api/v1/verification/username/rahul.sharma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("rahul.sharma"))
                .andExpect(jsonPath("$.overallStatus").exists())
                .andExpect(jsonPath("$.providerResults").isMap());
    }
}
