package com.twekl.dashboard.administration.controller.timefilter;

import com.twekl.dashboard.administration.config.JpaConfig;
import com.twekl.dashboard.administration.config.SecurityConfig;
import com.twekl.dashboard.administration.config.WebMvcConfig;
import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterCreateService;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterDeleteService;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterQueryService;
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
    private TimeFilterQueryService timeFilterQueryService;

    @Autowired
    private TimeFilterCreateService timeFilterCreateService;

    @Autowired
    private TimeFilterDeleteService timeFilterDeleteService;

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
        timeFilterQueryService.findAll().stream()
                .filter(p -> p.getPresetKey() != null && (
                        p.getPresetKey().startsWith("TEST_") ||
                        p.getPresetKey().startsWith("PRESET_") ||
                        p.getPresetKey().startsWith("CUSTOM_") ||
                        (p.getName() != null && p.getName().startsWith("Test 5 Days")) ||
                        (p.getName() != null && p.getName().startsWith("Toggle Test")) ||
                        (p.getName() != null && p.getName().startsWith("Delete Test"))
                ))
                .forEach(p -> {
                    try {
                        timeFilterDeleteService.deletePreset(p.getPresetId());
                    } catch (Exception ignored) {}
                });
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
        TimeFilterPreset preset = timeFilterCreateService.createPreset(new TimeFilterPreset("PRESET_TOGGLE_" + System.currentTimeMillis(), "Toggle Test", "تجربة", "تاقیکردنەوە", 10, "DAYS", 99));

        mockMvc.perform(post("/admin/timelines/" + preset.getPresetId() + "/toggle-status"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/timelines"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("POST /admin/timelines/{presetId}/delete-preset should delete preset and redirect")
    public void testDeletePresetSsr() throws Exception {
        TimeFilterPreset preset = timeFilterCreateService.createPreset(new TimeFilterPreset("PRESET_DELETE_" + System.currentTimeMillis(), "Delete Test", "حذف", "سڕینەوە", 15, "DAYS", 100));

        mockMvc.perform(post("/admin/timelines/" + preset.getPresetId() + "/delete-preset"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/timelines"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/time-filters should return JSON list of presets")
    public void testGetTimeFiltersRest() throws Exception {
        mockMvc.perform(get("/admin/time-filters").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("PATCH /admin/time-filters/{id}/toggle should toggle preset and return ApiResponse")
    public void testToggleTimeFilterRest() throws Exception {
        TimeFilterPreset preset = timeFilterCreateService.createPreset(new TimeFilterPreset("TEST_TOGGLE_REST_" + System.currentTimeMillis(), "Toggle Rest Test", "تجربة", "تاقیکردنەوە", 12, "DAYS", 99));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/admin/time-filters/" + preset.getPresetId() + "/toggle")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.active").value(false));
    }
}
