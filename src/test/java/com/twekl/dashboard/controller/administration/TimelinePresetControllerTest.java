package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.config.administration.JpaConfig;
import com.twekl.dashboard.security.administration.SecurityConfig;
import com.twekl.dashboard.config.administration.WebMvcConfig;
import com.twekl.dashboard.model.administration.TimeFilterPreset;
import com.twekl.dashboard.service.administration.TimeFilterService;
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
                .defaultRequest(get("/").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /admin/timelines unauthenticated should redirect to /login")
    public void testGetTimelinesUnauthenticated() throws Exception {
        mockMvc.perform(get("/admin/timelines").accept(org.springframework.http.MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/timelines authenticated should render administration/timefilter/timeFilters view")
    public void testGetTimelinesAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/timelines").accept(org.springframework.http.MediaType.TEXT_HTML))
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
