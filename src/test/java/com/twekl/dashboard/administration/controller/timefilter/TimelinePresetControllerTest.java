package com.twekl.dashboard.administration.controller.timefilter;

import com.twekl.dashboard.administration.config.JpaConfig;
import com.twekl.dashboard.administration.config.SecurityConfig;
import com.twekl.dashboard.administration.config.WebMvcConfig;
import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterPresetService;
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
public class TimelinePresetControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TimeFilterPresetService presetService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /admin/timelines without authentication should redirect to /login")
    public void testGetTimelinesHtmlUnauthenticated() throws Exception {
        mockMvc.perform(get("/admin/timelines"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/timelines as HTML should render administration/timefilter/timeFilters view")
    public void testGetTimelinesHtmlAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/timelines"))
                .andExpect(status().isOk())
                .andExpect(view().name("administration/timefilter/timeFilters"))
                .andExpect(model().attributeExists("presetList"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("POST /admin/timelines/create-preset SSR form submission should create preset and redirect with flash message")
    public void testCreatePresetSsrForm() throws Exception {
        mockMvc.perform(post("/admin/timelines/create-preset")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("labelEn", "Test 5 Days Followup")
                        .param("durationValue", "5")
                        .param("durationUnit", "DAYS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/timelines"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("POST /admin/timelines/{presetId}/toggle-status should toggle preset status and redirect")
    public void testTogglePresetSsr() throws Exception {
        TimeFilterPreset preset = presetService.createPreset(new TimeFilterPreset("PRESET_TOGGLE_" + System.currentTimeMillis(), "Toggle Test", "تجربة", "تاقیکردنەوە", 10, "DAYS", 99));

        mockMvc.perform(post("/admin/timelines/" + preset.getPresetId() + "/toggle-status"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/timelines"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("POST /admin/timelines/{presetId}/delete-preset should delete preset and redirect")
    public void testDeletePresetSsr() throws Exception {
        TimeFilterPreset preset = presetService.createPreset(new TimeFilterPreset("PRESET_DELETE_" + System.currentTimeMillis(), "Delete Test", "حذف", "سڕینەوە", 15, "DAYS", 100));

        mockMvc.perform(post("/admin/timelines/" + preset.getPresetId() + "/delete-preset"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/timelines"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
