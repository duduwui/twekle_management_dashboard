package com.twekl.dashboard.controller.administration;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.twekl.dashboard.config.administration.JpaConfig;
import com.twekl.dashboard.config.administration.SecurityConfig;
import com.twekl.dashboard.config.administration.WebMvcConfig;

import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith(SpringExtension.class)
@org.springframework.transaction.annotation.Transactional
@WebAppConfiguration
@org.springframework.test.context.ContextHierarchy({
    @ContextConfiguration(
            name = "root",
            classes = {JpaConfig.class, SecurityConfig.class}),
    @ContextConfiguration(name = "mvc", classes = WebMvcConfig.class)
})
public class CustomerControllerTest {

    @Autowired private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc =
                MockMvcBuilders.webAppContextSetup(context)
                        .defaultRequest(
                                get("/").with(
                                                org.springframework.security.test.web.servlet
                                                        .request
                                                        .SecurityMockMvcRequestPostProcessors
                                                        .csrf()))
                        .apply(springSecurity())
                        .build();
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = "SUPER_ADMIN")
    void directoryAndReportTabsRenderWithoutAngular() throws Exception {
        for (String path :
                java.util.List.of(
                        "/admin/followups",
                        "/admin/reports?tab=milestones",
                        "/admin/reports?tab=satisfaction",
                        "/admin/reports?tab=products",
                        "/admin/reports?tab=followups")) {
            mockMvc.perform(get(path).accept(MediaType.TEXT_HTML))
                    .andExpect(status().isOk())
                    .andExpect(
                            org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                    .content()
                                    .string(
                                            org.hamcrest.Matchers.not(
                                                    org.hamcrest.Matchers.containsString(
                                                            "angular.min.js"))));
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void noDatabasePermissionsMeansNoCustomerWrites() throws Exception {
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/admin/customers")
                                .param("name", "Denied")
                                .param("phoneNumber", "123"))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/admin/customers/1/delete"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/reports")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = "SUPER_ADMIN")
    void invalidCustomerFormPreservesErrors() throws Exception {
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/admin/customers")
                                .param("name", "")
                                .param("phoneNumber", "123"))
                .andExpect(status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.model()
                                .attributeHasFieldErrors("customerForm", "name"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = "SUPER_ADMIN")
    void unknownFiltersDoNotSilentlyLoadAllHistory() throws Exception {
        mockMvc.perform(get("/admin/followups").param("period", "typo"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/admin/reports").param("tab", "typo"))
                .andExpect(status().isBadRequest());
    }
}
