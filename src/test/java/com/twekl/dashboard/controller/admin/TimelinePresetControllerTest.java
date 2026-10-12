package com.twekl.dashboard.controller.admin;

import com.twekl.dashboard.config.JpaConfig;
import com.twekl.dashboard.config.SecurityConfig;
import com.twekl.dashboard.config.WebMvcConfig;
import com.twekl.dashboard.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.service.admin.TimeFilterService;
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
    private TimeFilterService timeFilterService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @org.junit.jupiter.api.AfterEach
    public void cleanupTestPresets() {
        timeFilterService.findAll().stream()
                .filter(p -> p.getLabelEn() != null && (
                        p.getLabelEn().startsWith("SSR_") ||
                        p.getLabelEn().startsWith("Test_") ||
                        p.getLabelEn().startsWith("Del_") ||
                        p.getLabelEn().startsWith("Toggle_")
                ))
                .forEach(p -> {
                    try {
                        timeFilterService.deletePreset(p.getId());
                    } catch (Exception ignored) {}
                });
    }

    @Test
    @DisplayName("GET /admin/timelines unauthenticated should redirect to /login")
    public void testGetTimelinesUnauthenticated() throws Exception {
        mockMvc.perform(get("/admin/timelines"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/timelines authenticated should render administration/timefilter/timeFilters view")
    public void testGetTimelinesAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/timelines"))
                .andExpect(status().isOk())
                .andExpect(view().name("administration/timefilter/timeFilters"))
                .andExpect(model().attributeExists("presetList"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("POST /admin/timelines/create SSR form submission should create preset and redirect")
    public void testCreatePresetSsrForm() throws Exception {
        String label = "SSR_Preset_" + System.currentTimeMillis();
        mockMvc.perform(post("/admin/timelines/create")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("labelEn", label)
                        .param("durationValue", "12")
                        .param("durationUnit", "HOURS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/timelines"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("POST /admin/timelines/{presetId}/toggle SSR should toggle preset status")
    public void testTogglePresetSsr() throws Exception {
        TimeFilterPreset preset = new TimeFilterPreset();
        preset.setLabelEn("Toggle_" + System.currentTimeMillis());
        preset.setDurationValue(5);
        preset.setDurationUnit("DAYS");
        preset.setIsActive(true);
        TimeFilterPreset created = timeFilterService.createPreset(preset);

        mockMvc.perform(post("/admin/timelines/" + created.getId() + "/toggle"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/timelines"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("POST /admin/timelines/{presetId}/delete SSR should delete preset")
    public void testDeletePresetSsr() throws Exception {
        TimeFilterPreset preset = new TimeFilterPreset();
        preset.setLabelEn("Del_" + System.currentTimeMillis());
        preset.setDurationValue(10);
        preset.setDurationUnit("DAYS");
        TimeFilterPreset created = timeFilterService.createPreset(preset);

        mockMvc.perform(post("/admin/timelines/" + created.getId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/timelines"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
