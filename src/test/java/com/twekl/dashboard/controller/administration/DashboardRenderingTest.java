package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.config.JpaConfig;
import com.twekl.dashboard.security.administration.SecurityConfig;
import com.twekl.dashboard.config.WebMvcConfig;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@org.springframework.test.context.ContextHierarchy({
        @ContextConfiguration(name = "root", classes = {JpaConfig.class, SecurityConfig.class}),
        @ContextConfiguration(name = "mvc", classes = WebMvcConfig.class)
})
@Transactional
class DashboardRenderingTest {
    @Autowired private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = "SUPER_ADMIN")
    void dashboardRendersDataWithoutAngularOrJsonBootstrap() throws Exception {
        mvc.perform(get("/admin/dashboard").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Priority Attention")))
                .andExpect(content().string(containsString("/admin/customers")))
                .andExpect(content().string(containsString("Customer Satisfaction")))
                .andExpect(content().string(containsString("Timeline Milestones")))
                .andExpect(content().string(not(containsString("angular.min.js"))))
                .andExpect(content().string(not(containsString("ng-controller"))))
                .andExpect(content().string(not(containsString("SERVER_STATS"))))
                .andExpect(content().string(not(containsString("{{"))));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = "SUPER_ADMIN")
    void statsSummaryReturnsChartsData() throws Exception {
        mvc.perform(get("/admin/stats/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.milestoneChart").isArray())
                .andExpect(jsonPath("$.satisfactionChart").isMap())
                .andExpect(jsonPath("$.satisfactionChart.satisfactionRate").exists());
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = "SUPER_ADMIN")
    void rootRedirectsToDashboard() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = "SUPER_ADMIN")
    void followupsRendersSuccessfully() throws Exception {
        mvc.perform(get("/admin/followups").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Follow-ups | Twekl Administration")));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = "SUPER_ADMIN")
    void reportsRendersSuccessfully() throws Exception {
        mvc.perform(get("/admin/reports").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reports &amp; Analytics")));
    }

    @Test
    void htmlRequestsRedirectToLoginButJsonRequestsReturnUnauthorized() throws Exception {
        mvc.perform(get("/admin/dashboard").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/admin/stats/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ordinaryAdminCannotReadStaffWithoutPermission() throws Exception {
        mvc.perform(get("/admin/admins").accept(MediaType.TEXT_HTML))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginIsAnHtmlFormAndNoBundledCredentials() throws Exception {
        mvc.perform(get("/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/login\"")))
                .andExpect(content().string(not(containsString("Default credentials"))))
                .andExpect(content().string(not(containsString("handleLogin"))));
    }
}
