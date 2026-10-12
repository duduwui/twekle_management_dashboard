package com.twekl.dashboard.administration.controller.admin;

import com.twekl.dashboard.administration.config.JpaConfig;
import com.twekl.dashboard.administration.config.SecurityConfig;
import com.twekl.dashboard.administration.config.WebMvcConfig;
import com.twekl.dashboard.administration.dto.admin.CreateAdminRequest;
import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.service.admin.AdminCreateService;
import com.twekl.dashboard.administration.service.admin.AdminDeleteService;
import com.twekl.dashboard.administration.service.admin.AdminQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {JpaConfig.class, WebMvcConfig.class, SecurityConfig.class})
public class AdminStaffControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AdminQueryService adminQueryService;

    @Autowired
    private AdminCreateService adminCreateService;

    @Autowired
    private AdminDeleteService adminDeleteService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @org.junit.jupiter.api.AfterEach
    public void cleanupTestAdmins() {
        adminQueryService.findAll().stream()
                .filter(a -> a.getUsername() != null && (
                        a.getUsername().startsWith("ssr_") ||
                        a.getUsername().startsWith("toggle_") ||
                        a.getUsername().startsWith("delete_") ||
                        a.getUsername().contains("test")
                ))
                .forEach(a -> {
                    try {
                        adminDeleteService.deleteAdmin(a.getAdminId());
                    } catch (Exception ignored) {}
                });
    }

    @Test
    @DisplayName("GET /admin/admins without authentication should redirect to /login")
    public void testGetAdminsHtmlUnauthenticated() throws Exception {
        mockMvc.perform(get("/admin/admins"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/admins as HTML should render administration/admin/admins view")
    public void testGetAdminsHtmlAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/admins"))
                .andExpect(status().isOk())
                .andExpect(view().name("administration/admin/admins"))
                .andExpect(model().attributeExists("adminList"))
                .andExpect(model().attributeExists("roleList"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", authorities = {"ROLE_SUPER_ADMIN", "PERMISSION_MANAGE_USERS"})
    @DisplayName("POST /admin/admins/create-admin SSR form submission should create admin and redirect with flash message")
    public void testCreateAdminSsrForm() throws Exception {
        String testUser = "ssr_test_" + System.currentTimeMillis();
        mockMvc.perform(post("/admin/admins/create-admin")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", testUser)
                        .param("password", "Pass@123456")
                        .param("phoneNumber", "+966 50 999 8888"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/admins"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", authorities = {"ROLE_SUPER_ADMIN", "PERMISSION_MANAGE_USERS"})
    @DisplayName("POST /admin/admins/{adminId}/toggle-status should toggle status and redirect with flash message")
    public void testToggleAdminStatusSsr() throws Exception {
        CreateAdminRequest req = new CreateAdminRequest();
        req.setUsername("toggle_test_" + System.currentTimeMillis());
        req.setPassword("Pass@123456");
        Admin admin = adminCreateService.createAdmin(req);

        mockMvc.perform(post("/admin/admins/" + admin.getAdminId() + "/toggle-status"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/admins"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", authorities = {"ROLE_SUPER_ADMIN", "PERMISSION_MANAGE_USERS"})
    @DisplayName("POST /admin/admins/{adminId}/delete-admin should delete admin and redirect with flash message")
    public void testDeleteAdminSsr() throws Exception {
        CreateAdminRequest req = new CreateAdminRequest();
        req.setUsername("delete_test_" + System.currentTimeMillis());
        req.setPassword("Pass@123456");
        Admin admin = adminCreateService.createAdmin(req);

        mockMvc.perform(post("/admin/admins/" + admin.getAdminId() + "/delete-admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/admins"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
