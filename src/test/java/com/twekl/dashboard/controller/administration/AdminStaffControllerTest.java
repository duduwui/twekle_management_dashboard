package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.config.administration.JpaConfig;
import com.twekl.dashboard.config.administration.SecurityConfig;
import com.twekl.dashboard.config.administration.WebMvcConfig;
import com.twekl.dashboard.dto.administration.CreateAdminRequest;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.service.administration.AdminService;
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
@org.springframework.transaction.annotation.Transactional
@WebAppConfiguration
@ContextConfiguration(classes = {JpaConfig.class, WebMvcConfig.class, SecurityConfig.class})
public class AdminStaffControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AdminService adminService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .defaultRequest(get("/").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /admin/admins without authentication should redirect to /login")
    public void testGetAdminsHtmlUnauthenticated() throws Exception {
        mockMvc.perform(get("/admin/admins").accept(org.springframework.http.MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/admins as HTML should render administration/admin/admins view")
    public void testGetAdminsHtmlAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/admins").accept(org.springframework.http.MediaType.TEXT_HTML))
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
        Admin admin = adminService.createAdmin(req);

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
        Admin admin = adminService.createAdmin(req);

        mockMvc.perform(post("/admin/admins/" + admin.getAdminId() + "/delete-admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/admins"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
