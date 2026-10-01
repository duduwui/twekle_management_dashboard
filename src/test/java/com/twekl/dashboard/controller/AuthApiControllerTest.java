package com.twekl.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twekl.dashboard.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/auth/login with valid super admin credentials should succeed")
    public void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("twekl_super_admin");
        request.setPassword("Super@2026");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("twekl_super_admin"))
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.superAdmin").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid password should return 403")
    public void testLoginInvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("twekl_super_admin");
        request.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("GET /api/auth/current for unauthenticated user should return authenticated=false")
    public void testGetCurrentUserUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /api/auth/current for authenticated user should return role permissions")
    public void testGetCurrentUserAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("twekl_super_admin"))
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"));
    }
}
