package com.twekl.dashboard.administration.controller.admin;

import com.twekl.dashboard.administration.config.JpaConfig;
import com.twekl.dashboard.administration.config.SecurityConfig;
import com.twekl.dashboard.administration.config.WebMvcConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {JpaConfig.class, WebMvcConfig.class, SecurityConfig.class})
public class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /admin/auth/me for unauthenticated user should return 401 Unauthorized")
    public void testGetCurrentUserUnauthenticated() throws Exception {
        mockMvc.perform(get("/admin/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/auth/me for authenticated user should return profile")
    public void testGetCurrentUserAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("twekl_super_admin"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /internal/auth/me compatibility alias should return profile")
    public void testGetCurrentUserInternalAlias() throws Exception {
        mockMvc.perform(get("/internal/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("twekl_super_admin"));
    }
}

