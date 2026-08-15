package com.accesszero.controller;

import com.accesszero.domain.entity.UserEntity;
import com.accesszero.dto.ContainmentRequestDto;
import com.accesszero.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ContainmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testContainmentEndpoints() throws Exception {
        UserEntity user = userRepository.findByUsername("rahul.sharma").orElseThrow();

        // Request containment with emergency override
        ContainmentRequestDto request = new ContainmentRequestDto(
                user.getId(),
                user.getUsername(),
                "it.tester",
                "API Unit Test Containment",
                true
        );

        mockMvc.perform(post("/api/v1/containment/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationId").exists())
                .andExpect(jsonPath("$.username").value("rahul.sharma"));

        // List operations
        mockMvc.perform(get("/api/v1/containment/operations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
