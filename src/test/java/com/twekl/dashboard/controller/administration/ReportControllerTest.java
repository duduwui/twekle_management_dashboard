package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.config.administration.JpaConfig;
import com.twekl.dashboard.config.administration.WebMvcConfig;
import com.twekl.dashboard.security.administration.SecurityConfig;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(SpringExtension.class)
@Transactional
@WebAppConfiguration
@ContextConfiguration(classes = {JpaConfig.class, WebMvcConfig.class, SecurityConfig.class})
public class ReportControllerTest {

    @Autowired
    private WebApplicationContext context;

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
    @DisplayName("GET /admin/reports unauthenticated should redirect to /login")
    public void testGetReportsUnauthenticated() throws Exception {
        mockMvc.perform(get("/admin/reports").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/reports authenticated renders reports view with model")
    public void testGetReportsAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/reports").param("tab", "milestones").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("administration/report/reports"))
                .andExpect(model().attribute("activeTab", "reports"))
                .andExpect(model().attribute("reportSubTab", "milestones"))
                .andExpect(model().attributeExists("reportData"))
                .andExpect(model().attributeExists("presets"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/reports/data as JSON returns analytics payload")
    public void testGetAnalyticsReportData() throws Exception {
        mockMvc.perform(get("/admin/reports/data").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/reports/satisfaction returns satisfaction scores")
    public void testGetSatisfactionDetails() throws Exception {
        mockMvc.perform(get("/admin/reports/satisfaction").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/reports/export generates CSV attachment")
    public void testExportSatisfactionCsv() throws Exception {
        mockMvc.perform(get("/admin/reports/export").param("period", "ALL"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"satisfaction-report-ALL.csv\"")))
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(content().string(containsString("ID,Customer Name,Phone,City,Order ID,Order Number,Rating,Feedback Date,Comment")));
    }
}
