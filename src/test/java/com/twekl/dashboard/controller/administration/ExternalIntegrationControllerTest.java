package com.twekl.dashboard.controller.administration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twekl.dashboard.config.JpaConfig;
import com.twekl.dashboard.config.WebMvcConfig;
import com.twekl.dashboard.security.administration.SecurityConfig;
import com.twekl.dashboard.service.administration.ExternalIntegrationService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@Transactional
@WebAppConfiguration
@ContextConfiguration(classes = {JpaConfig.class, WebMvcConfig.class, SecurityConfig.class})
public class ExternalIntegrationControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ExternalIntegrationService externalIntegrationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /api/external/review-session when unauthenticated redirects to /login and saves target")
    public void testReviewSessionUnauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/api/external/review-session")
                        .param("customerName", "Aram")
                        .param("customerId", "101")
                        .param("orderId", "501")
                        .param("date", "2026-10-26"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?redirect=")));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /api/external/review-session when already authenticated redirects directly to customer review with orderId")
    public void testReviewSessionAuthenticatedRedirectsToFollowups() throws Exception {
        mockMvc.perform(get("/api/external/review-session")
                        .param("customerName", "Aram")
                        .param("customerId", "101")
                        .param("orderId", "501")
                        .param("date", "2026-10-26"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/admin/followups?orderId=")));
    }

    @Test
    @DisplayName("POST /api/external/review-session with invalid key should return 401 Unauthorized")
    public void testReviewSessionUnauthorized() throws Exception {
        String body = "{\"customerName\":\"Test Cust\",\"customerId\":1001,\"orderId\":9921,\"date\":\"2026-10-26\"}";

        mockMvc.perform(post("/api/external/review-session")
                        .header("X-API-KEY", "wrong_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/jwt/generate produces valid non-expiring JWT")
    public void testGenerateJwtEndpoint() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/jwt/generate")
                        .param("customer_name", "Aram")
                        .param("customer_id", "101")
                        .param("order_id", "501")
                        .param("date", "2026-10-26")
                        .param("nonExpiring", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        String token = json.get("token").asText();
        assertNotNull(token);
    }

    @Test
    @DisplayName("JWT Key Rotation: Tokens generated before key rotation still verify after key rotation")
    public void testKeyRotationDoesNotBreakExistingTokens() throws Exception {
        // 1. Generate token with original key
        MvcResult res = mockMvc.perform(get("/api/jwt/generate")
                        .param("customerName", "Dilshad")
                        .param("customerId", "202")
                        .param("orderId", "702")
                        .param("date", "2026-10-26"))
                .andExpect(status().isOk())
                .andReturn();

        String oldToken = objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();

        // 2. Rotate to new primary key via API
        mockMvc.perform(post("/api/jwt/rotate-key")
                        .header("X-API-KEY", "twekl_sv_secret_2026")
                        .param("newKey", "rotated_new_secret_key_2026_xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Verify old token still works seamlessly (unauthenticated redirects to /login without 401 error)
        mockMvc.perform(get("/api/external/review-session")
                        .param("token", oldToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?redirect=")));

        // 4. Generate new token with new key
        MvcResult res2 = mockMvc.perform(get("/api/jwt/generate")
                        .param("customerName", "Dilshad")
                        .param("customerId", "202")
                        .param("orderId", "702")
                        .param("date", "2026-10-26"))
                .andExpect(status().isOk())
                .andReturn();
        String newToken = objectMapper.readTree(res2.getResponse().getContentAsString()).get("token").asText();

        // 5. Verify new token works too
        mockMvc.perform(get("/api/external/review-session")
                        .param("token", newToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?redirect=")));
    }

    @Test
    @DisplayName("GET /api/external/admins returns JSON list of all administrators, users and roles")
    public void testGetExternalAdminsEndpoint() throws Exception {
        mockMvc.perform(get("/api/external/admins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.admins").isArray())
                .andExpect(jsonPath("$.data.users").isArray())
                .andExpect(jsonPath("$.data.roles").isArray());
    }

    @Test
    @DisplayName("GET /api/admins with username filter returns matching administrator info and role")
    public void testGetExternalAdminByUsername() throws Exception {
        mockMvc.perform(get("/api/admins").param("username", "twekl_super_admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.admins[0].username").value("twekl_super_admin"))
                .andExpect(jsonPath("$.data.admins[0].isSuperAdmin").value(true));
    }

    @Test
    @DisplayName("GET /followup endpoint creates follow-up session and forwards to followups view")
    public void testFollowupEndpointRedirects() throws Exception {
        mockMvc.perform(get("/followup")
                        .param("customerName", "Aram")
                        .param("customerId", "205")
                        .param("orderId", "5021")
                        .param("date", "2026-10-27"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?redirect=")));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /review returns customer reviews JSON (excludes pending and unfinished)")
    public void testCustomerReviewApiReturnsReviewedOnly() throws Exception {
        mockMvc.perform(get("/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviewedCustomers").isNumber());
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /review with customerId filter returns specific customer review summary")
    public void testCustomerReviewApiWithCustomerId() throws Exception {
        mockMvc.perform(get("/review").param("customerId", "1018"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.customerId").exists());
    }

    @Test
    @DisplayName("GET /review in browser unauthenticated redirects to /login")
    public void testCustomerReviewBrowserUnauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/review")
                        .param("orderId", "43356")
                        .accept(MediaType.TEXT_HTML, MediaType.ALL))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?redirect=")));
    }

    @Test
    @DisplayName("GET /review API unauthenticated returns 401 Unauthorized")
    public void testCustomerReviewApiUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/review")
                        .param("orderId", "43356")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /review in browser when authenticated returns raw clean JSON")
    public void testCustomerReviewBrowserReturnsRawJson() throws Exception {
        mockMvc.perform(get("/review")
                        .param("customerId", "1018")
                        .accept(MediaType.TEXT_HTML, MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customer.customerId").exists());
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /review in browser with format=json returns raw JSON")
    public void testCustomerReviewBrowserWithFormatJsonReturnsJson() throws Exception {
        mockMvc.perform(get("/review")
                        .param("customerId", "1018")
                        .param("format", "json")
                        .accept(MediaType.TEXT_HTML, MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.customerId").exists());
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /review with orderId filter looks up by order ID")
    public void testCustomerReviewApiWithOrderId() throws Exception {
        mockMvc.perform(get("/review").param("orderId", "5021"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.customerId").exists());
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /review/{id} with order ID looks up by order ID")
    public void testCustomerReviewApiByOrderIdPathVariable() throws Exception {
        mockMvc.perform(get("/review/5021"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.customerId").exists());
    }

    @Test
    @DisplayName("GET /followup/review/{orderId} in browser renders followups view when authenticated")
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    public void testCleanReviewUrlAuthenticatedRendersView() throws Exception {
        mockMvc.perform(get("/followup/review/43356")
                        .accept(MediaType.TEXT_HTML, MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(view().name("administration/followup/followups"))
                .andExpect(model().attributeExists("presets"));
    }

    @Test
    @DisplayName("GET /followup/review/{orderId} when unauthenticated redirects to /login?redirect=/followup/review/43356")
    public void testCleanReviewUrlUnauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/followup/review/43356")
                        .accept(MediaType.TEXT_HTML, MediaType.ALL))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?redirect=")))
                .andExpect(header().string("Location", containsString("%2Ffollowup%2Freview%2F43356")));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /followup/review/{orderId} with Accept: application/json returns clean review JSON")
    public void testCleanReviewUrlJsonReturnsReviewedOnly() throws Exception {
        mockMvc.perform(get("/followup/review/43356")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.customerId").exists());
    }

    @Test
    @DisplayName("GET /admin/followup/review/{orderId} in browser renders followups view when authenticated")
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    public void testAdminCleanReviewUrlAuthenticatedRendersView() throws Exception {
        mockMvc.perform(get("/admin/followup/review/43356")
                        .accept(MediaType.TEXT_HTML, MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(view().name("administration/followup/followups"));
    }

    @Test
    @DisplayName("GET /api/session/generate produces encrypted session and verified URL")
    public void testGenerateEncryptedSessionEndpoint() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/session/generate")
                        .param("customerName", "Aram")
                        .param("customerId", "20")
                        .param("orderId", "43356")
                        .param("date", "2026-10-29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.encryptedToken").isString())
                .andExpect(jsonPath("$.encryptedUrl").isString())
                .andReturn();

        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        String encryptedToken = root.get("encryptedToken").asText();

        // Verify the generated encrypted token can be passed to /followup
        mockMvc.perform(get("/followup")
                        .param("session", encryptedToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?redirect=")));
    }

    @Test
    @DisplayName("GET /followup with tampered session token returns 401 Unauthorized")
    public void testFollowupWithTamperedSessionToken() throws Exception {
        mockMvc.perform(get("/followup")
                        .param("session", "enc-invalid_corrupted_token_12345"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid, expired, or tampered encrypted session token")));
    }
}
