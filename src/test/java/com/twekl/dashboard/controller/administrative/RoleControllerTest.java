package com.twekl.dashboard.controller.administrative;

import com.twekl.dashboard.config.JpaConfig;
import com.twekl.dashboard.config.SecurityConfig;
import com.twekl.dashboard.config.WebMvcConfig;
import com.twekl.dashboard.dto.role.RoleDto;
import com.twekl.dashboard.model.role.Role;
import com.twekl.dashboard.service.administrative.RoleService;
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

    @org.junit.jupiter.api.AfterEach
    public void cleanupTestRoles() {
        roleService.findAll().stream()
                .filter(r -> r.getName() != null && (
                        r.getName().startsWith("Test_") ||
                        r.getName().startsWith("SSR_") ||
                        r.getName().startsWith("Assigned_") ||
                        r.getName().startsWith("Del_") ||
                        r.getName().contains("Audit")
                ))
                .forEach(r -> {
                    try {
                        roleService.deleteRole(r.getRoleId());
                    } catch (Exception ignored) {}
                });
    }

    @Test
    @DisplayName("GET /admin/roles unauthenticated should redirect to /login")
    public void testGetRolesUnauthenticated() throws Exception {
        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/roles authenticated should render administration/role/roles view")
    public void testGetRolesAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(view().name("administration/role/roles"))
                .andExpect(model().attributeExists("roleList"))
                .andExpect(model().attributeExists("adminList"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", authorities = {"ROLE_SUPER_ADMIN", "PERMISSION_CREATE_ROLES"})
    @DisplayName("POST /admin/roles/create SSR form submission should create role and redirect with flash message")
    public void testCreateRoleSsrForm() throws Exception {
        String roleName = "SSR_Role_" + System.currentTimeMillis();
        mockMvc.perform(post("/admin/roles/create")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", roleName)
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
    @DisplayName("POST /admin/roles/{roleId}/update SSR form should update role permissions and redirect")
    public void testUpdateRoleSsrForm() throws Exception {
        RoleDto dto = new RoleDto("SSR_Upd_" + System.currentTimeMillis(), true, true, false, false);
        Role role = roleService.createRole(dto);

        mockMvc.perform(post("/admin/roles/" + role.getRoleId() + "/update")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", role.getName())
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
    @DisplayName("POST /admin/roles/{roleId}/delete SSR should delete custom role and redirect")
    public void testDeleteRoleSsr() throws Exception {
        RoleDto dto = new RoleDto("Del_Role_" + System.currentTimeMillis(), false, true, false, false);
        Role role = roleService.createRole(dto);

        mockMvc.perform(post("/admin/roles/" + role.getRoleId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
