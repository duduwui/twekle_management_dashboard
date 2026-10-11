package com.twekl.dashboard.administration.controller.role;

import com.twekl.dashboard.administration.config.JpaConfig;
import com.twekl.dashboard.administration.config.SecurityConfig;
import com.twekl.dashboard.administration.config.WebMvcConfig;
import com.twekl.dashboard.administration.model.role.Role;
import com.twekl.dashboard.administration.service.role.RoleService;
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
public class RoleControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RoleService roleService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /admin/roles without authentication should redirect to /login")
    public void testGetRolesHtmlUnauthenticated() throws Exception {
        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/roles as HTML should render administration/role/roles view")
    public void testGetRolesHtmlAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(view().name("administration/role/roles"))
                .andExpect(model().attributeExists("roleList"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", authorities = {"ROLE_SUPER_ADMIN", "PERMISSION_CREATE_ROLES"})
    @DisplayName("POST /admin/roles/create-role SSR form submission should create role and redirect with flash message")
    public void testCreateRoleSsrForm() throws Exception {
        String testRoleName = "SSR Test Role " + System.currentTimeMillis();
        mockMvc.perform(post("/admin/roles/create-role")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", testRoleName)
                        .param("canCreate", "true")
                        .param("canRead", "true")
                        .param("canUpdate", "false")
                        .param("canDelete", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", authorities = {"ROLE_SUPER_ADMIN", "PERMISSION_CREATE_ROLES"})
    @DisplayName("POST /admin/roles/{roleId}/update-role SSR form submission should update role and redirect")
    public void testUpdateRoleSsrForm() throws Exception {
        Role created = roleService.createRole(new Role("Test Update Role " + System.currentTimeMillis(), true, true, false, false));

        mockMvc.perform(post("/admin/roles/" + created.getRoleId() + "/update-role")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", created.getName() + " Updated")
                        .param("canCreate", "true")
                        .param("canRead", "true")
                        .param("canUpdate", "true")
                        .param("canDelete", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", authorities = {"ROLE_SUPER_ADMIN", "PERMISSION_CREATE_ROLES"})
    @DisplayName("POST /admin/roles/{roleId}/delete-role SSR form submission should delete role and redirect")
    public void testDeleteRoleSsrForm() throws Exception {
        Role created = roleService.createRole(new Role("Test Delete Role " + System.currentTimeMillis(), false, true, false, false));

        mockMvc.perform(post("/admin/roles/" + created.getRoleId() + "/delete-role"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
