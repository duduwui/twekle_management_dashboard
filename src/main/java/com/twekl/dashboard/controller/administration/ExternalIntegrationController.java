package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.AdminUserDto;
import com.twekl.dashboard.dto.administration.ExternalReviewRequest;
import com.twekl.dashboard.dto.administration.LoginRequest;
import com.twekl.dashboard.response.AuthResponse;
import com.twekl.dashboard.dto.administration.RoleDto;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.response.ApiResponse;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.ExternalIntegrationService;
import com.twekl.dashboard.service.administration.RoleService;
import com.twekl.dashboard.service.administration.TimeFilterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class ExternalIntegrationController {

    private static final Logger log = LoggerFactory.getLogger(ExternalIntegrationController.class);

    private final ExternalIntegrationService externalIntegrationService;
    private final AdminService adminService;
    private final RoleService roleService;
    private final TimeFilterService timeFilterService;

    @Autowired(required = false)
    private com.twekl.dashboard.service.administration.AuthService authService;

    @Autowired
    public ExternalIntegrationController(ExternalIntegrationService externalIntegrationService,
                                         AdminService adminService,
                                         RoleService roleService,
                                         TimeFilterService timeFilterService) {
        this.externalIntegrationService = externalIntegrationService;
        this.adminService = adminService;
        this.roleService = roleService;
        this.timeFilterService = timeFilterService;
    }

    /**
     * External Review Entrypoint.
     * Receives the 4 customer data points from the SV's website:
     * - customerName
     * - customerId
     * - orderId
     * - date
     * (Optionally token / apiKey)
     *
     * Flow:
     * 1. Creates/verifies customer & order in Twekl database.
     * 2. If user is already logged in on Twekl -> opens follow-up log with that order expanded and review modal open.
     * 3. If user is NOT logged in on Twekl -> redirects to Twekl /login. After logging in with Twekl credentials,
     *    automatically forwards directly to that customer's follow-up log with that order expanded and review modal open.
     */
    @GetMapping(value = {
            "/followup",
            "/api/followup",
            "/api/external/followup",
            "/api/external/followup-session",
            "/api/external/review-session",
            "/admin/external/launch"
    })
    public Object launchReviewSession(
            @RequestParam(value = "session", required = false) String sessionToken,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "encryptedSession", required = false) String encryptedSession,
            @RequestParam(value = "enc", required = false) String encParam,
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "customer_name", required = false) String customerNameSnake,
            @RequestParam(value = "customername", required = false) String customerNameLower,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "customerId", required = false) String customerId,
            @RequestParam(value = "customer_id", required = false) String customerIdSnake,
            @RequestParam(value = "customerid", required = false) String customerIdLower,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "order_id", required = false) String orderIdSnake,
            @RequestParam(value = "orderid", required = false) String orderIdLower,
            @RequestParam(value = "orderNumber", required = false) String orderNumber,
            @RequestParam(value = "order_number", required = false) String orderNumberSnake,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "orderDate", required = false) String orderDate,
            @RequestParam(value = "order_date", required = false) String orderDateSnake,
            @RequestParam(value = "orderdate", required = false) String orderDateLower,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestHeader(value = "X-API-KEY", required = false) String apiKeyHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request,
            HttpServletResponse response) {

        ExternalReviewRequest reviewReq = null;

        // 1. If an encrypted session or JWT token was provided, decrypt & verify it cryptographically
        String providedToken = firstNonEmpty(sessionToken, token, encryptedSession, encParam);
        if (providedToken != null && !providedToken.trim().isEmpty()) {
            reviewReq = externalIntegrationService.decryptSession(providedToken.trim());
            if (reviewReq == null) {
                log.warn("Invalid, expired, or tampered encrypted session rejected on /followup: {}", providedToken);
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("success", false);
                err.put("message", "Invalid, expired, or tampered encrypted session token. Access Denied.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
        }

        // 2. Resolve parameters (merging claims from JWT with query parameters)
        String resolvedCustName = firstNonEmpty(
                reviewReq != null ? reviewReq.getCustomerName() : null,
                customerName, customerNameSnake, customerNameLower, name, "Customer"
        );
        String resolvedCustId = firstNonEmpty(
                reviewReq != null ? reviewReq.getCustomerId() : null,
                customerId, customerIdSnake, customerIdLower, id
        );
        String resolvedOrderId = firstNonEmpty(
                reviewReq != null ? reviewReq.getOrderId() : null,
                orderId, orderIdSnake, orderIdLower, orderNumber, orderNumberSnake
        );
        String resolvedDate = firstNonEmpty(
                reviewReq != null ? reviewReq.getDate() : null,
                date, orderDate, orderDateSnake, orderDateLower
        );
        resolvedDate = extractDateFromRequest(request, resolvedDate);

        if (reviewReq == null) {
            reviewReq = new ExternalReviewRequest();
        }
        reviewReq.setCustomerName(resolvedCustName);
        reviewReq.setCustomerId(resolvedCustId);
        reviewReq.setOrderId(resolvedOrderId);
        reviewReq.setDate(resolvedDate);

        log.info("Review request received [query='{}']: customerName='{}', customerId='{}', orderId='{}', date='{}'",
                request.getQueryString(), resolvedCustName, resolvedCustId, resolvedOrderId, resolvedDate);

        String baseUrl = extractBaseUrl(request);
        Map<String, Object> sessionData = externalIntegrationService.createReviewSession(reviewReq, baseUrl);

        Long custId = (Long) sessionData.get("customerId");
        Long ordId = (Long) sessionData.get("orderId");
        String sessionOrderNum = (String) sessionData.get("orderNumber");
        String reqOrderId = reviewReq.getOrderId();
        String finalOrderParam = (reqOrderId != null && !reqOrderId.trim().isEmpty())
                ? reqOrderId.trim()
                : ((sessionOrderNum != null && !sessionOrderNum.trim().isEmpty()) ? sessionOrderNum : String.valueOf(ordId));

        String targetUrl = "/admin/followups?orderId=" + finalOrderParam;

        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("application/json") && !acceptHeader.contains("text/html")) {
            return ResponseEntity.ok(sessionData);
        }

        // Check if the current browser session is authenticated on Twekl
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());

        if (isAuthenticated) {
            log.info("User already logged into Twekl. Opening follow-up log directly: {}", targetUrl);
            return "redirect:" + targetUrl;
        } else {
            log.info("User not logged into Twekl yet. Prompting login and saving target: {}", targetUrl);
            request.getSession(true).setAttribute("TARGET_AFTER_LOGIN", targetUrl);
            try {
                jakarta.servlet.http.Cookie c = new jakarta.servlet.http.Cookie("TARGET_AFTER_LOGIN", java.net.URLEncoder.encode(targetUrl, java.nio.charset.StandardCharsets.UTF_8));
                c.setPath("/");
                c.setMaxAge(600);
                response.addCookie(c);
            } catch (Exception ignored) {}
            return "redirect:/login?redirect=" + java.net.URLEncoder.encode(targetUrl, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * Browser Form or AJAX POST Endpoint.
     */
    @PostMapping(value = {
            "/followup",
            "/api/followup",
            "/api/external/followup",
            "/api/external/followup-session",
            "/api/external/review-session"
    })
    public Object createReviewSessionPost(
            @RequestBody(required = false) ExternalReviewRequest requestBody,
            @RequestParam(value = "session", required = false) String sessionToken,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "encryptedSession", required = false) String encryptedSession,
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "customer_name", required = false) String customerNameSnake,
            @RequestParam(value = "customerId", required = false) String customerId,
            @RequestParam(value = "customer_id", required = false) String customerIdSnake,
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "order_id", required = false) String orderIdSnake,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "orderDate", required = false) String orderDate,
            @RequestParam(value = "apiKey", required = false) String apiKeyParam,
            @RequestHeader(value = "X-API-KEY", required = false) String apiKeyHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request,
            HttpServletResponse response) {

        String key = resolveApiKey(apiKeyHeader, authHeader, apiKeyParam);
        if (key != null && !key.trim().isEmpty() && !externalIntegrationService.isValidApiKey(key)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("message", "Unauthorized. Invalid API key provided.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        ExternalReviewRequest reviewReq = requestBody;

        String providedToken = firstNonEmpty(sessionToken, token, encryptedSession);
        if (providedToken != null && !providedToken.trim().isEmpty()) {
            ExternalReviewRequest decrypted = externalIntegrationService.decryptSession(providedToken.trim());
            if (decrypted == null) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("success", false);
                err.put("message", "Invalid, expired, or tampered encrypted session token. Access Denied.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            reviewReq = decrypted;
        }

        if (reviewReq == null) {
            String custName = firstNonEmpty(customerName, customerNameSnake, "Customer");
            String custId = firstNonEmpty(customerId, customerIdSnake);
            String ordId = firstNonEmpty(orderId, orderIdSnake);
            String ordDate = firstNonEmpty(date, orderDate);

            reviewReq = new ExternalReviewRequest();
            reviewReq.setCustomerName(custName);
            reviewReq.setCustomerId(custId);
            reviewReq.setOrderId(ordId);
            reviewReq.setDate(ordDate);
        }

        String baseUrl = extractBaseUrl(request);
        Map<String, Object> sessionData = externalIntegrationService.createReviewSession(reviewReq, baseUrl);

        Long custId = (Long) sessionData.get("customerId");
        Long ordId = (Long) sessionData.get("orderId");
        String sessionOrderNum = (String) sessionData.get("orderNumber");
        String reqOrderId = reviewReq.getOrderId();
        String finalOrderParam = (reqOrderId != null && !reqOrderId.trim().isEmpty())
                ? reqOrderId.trim()
                : ((sessionOrderNum != null && !sessionOrderNum.trim().isEmpty()) ? sessionOrderNum : String.valueOf(ordId));

        String targetUrl = "/admin/followups?orderId=" + finalOrderParam;

        String contentType = request.getContentType();
        String acceptHeader = request.getHeader("Accept");

        // If JSON API request, return JSON
        if (contentType != null && contentType.contains("application/json") &&
            (acceptHeader == null || !acceptHeader.contains("text/html"))) {
            return ResponseEntity.ok(sessionData);
        }

        // If Form submit from browser:
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());

        if (isAuthenticated) {
            return "redirect:" + targetUrl;
        } else {
            request.getSession(true).setAttribute("TARGET_AFTER_LOGIN", targetUrl);
            try {
                jakarta.servlet.http.Cookie c = new jakarta.servlet.http.Cookie("TARGET_AFTER_LOGIN", java.net.URLEncoder.encode(targetUrl, java.nio.charset.StandardCharsets.UTF_8));
                c.setPath("/");
                c.setMaxAge(600);
                response.addCookie(c);
            } catch (Exception ignored) {}
            return "redirect:/login?redirect=" + java.net.URLEncoder.encode(targetUrl, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * Non-expiring JWT Generation Endpoint for Developer
     */
    @RequestMapping(value = "/api/jwt/generate", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateJwt(
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "customer_name", required = false) String customerNameSnake,
            @RequestParam(value = "customername", required = false) String customerNameLower,
            @RequestParam(value = "customerId", required = false) String customerId,
            @RequestParam(value = "customer_id", required = false) String customerIdSnake,
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "order_id", required = false) String orderIdSnake,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "orderDate", required = false) String orderDate,
            @RequestParam(value = "nonExpiring", defaultValue = "true") boolean nonExpiring,
            HttpServletRequest request) {

        ExternalReviewRequest reviewReq = new ExternalReviewRequest();
        String cName = firstNonEmpty(customerName, customerNameSnake, customerNameLower);
        String cId = firstNonEmpty(customerId, customerIdSnake);
        String oId = firstNonEmpty(orderId, orderIdSnake);
        String d = firstNonEmpty(date, orderDate, LocalDate.now().toString());

        if (cName != null) reviewReq.setCustomerName(cName);
        if (cId != null) reviewReq.setCustomerId(cId);
        if (oId != null) reviewReq.setOrderId(oId);
        reviewReq.setDate(d);

        String token = externalIntegrationService.generateReviewJwt(reviewReq, nonExpiring);

        String baseUrl = extractBaseUrl(request);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("token", token);
        resp.put("tokenType", "Bearer");
        resp.put("nonExpiring", nonExpiring);
        resp.put("reviewUrl", baseUrl + "/review?token=" + token);
        if (cId != null) {
            resp.put("customerReviewUrl", baseUrl + "/review?customerId=" + cId + "&token=" + token);
        }
        if (oId != null) {
            resp.put("orderReviewUrl", baseUrl + "/review?orderId=" + oId + "&token=" + token);
        }
        return ResponseEntity.ok(resp);
    }

    /**
     * Session Encryption & Generation Endpoint for the Supervisor.
     * Takes customerName, customerId, orderId, date, and returns:
     * - AES-256-GCM authenticated encrypted session token
     * - HMAC-SHA256 JWT session token
     * - Ready-to-use encrypted session URLs for /followup
     *
     * Example Endpoints:
     *   GET  /api/session/generate?customerName=Aram&customerId=20&orderId=43356&date=2026-10-29
     *   POST /api/session/generate
     *   GET  /api/session/encrypt
     */
    @RequestMapping(value = {
            "/api/session/generate",
            "/api/session/encrypt",
            "/api/external/session/generate",
            "/api/external/session/encrypt"
    }, method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateEncryptedSession(
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "customer_name", required = false) String customerNameSnake,
            @RequestParam(value = "customerId", required = false) String customerId,
            @RequestParam(value = "customer_id", required = false) String customerIdSnake,
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "order_id", required = false) String orderIdSnake,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "orderDate", required = false) String orderDate,
            HttpServletRequest request) {

        ExternalReviewRequest req = new ExternalReviewRequest();
        req.setCustomerName(firstNonEmpty(customerName, customerNameSnake, "Aram"));
        req.setCustomerId(firstNonEmpty(customerId, customerIdSnake, "20"));
        req.setOrderId(firstNonEmpty(orderId, orderIdSnake, "43356"));
        req.setDate(firstNonEmpty(date, orderDate, LocalDate.now().toString()));

        String encryptedToken = externalIntegrationService.encryptSession(req);
        String jwtToken = externalIntegrationService.generateReviewJwt(req, true);

        String baseUrl = extractBaseUrl(request);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "Encrypted session created successfully");
        resp.put("encryptedToken", encryptedToken);
        resp.put("jwtToken", jwtToken);
        resp.put("encryptedUrl", baseUrl + "/followup?session=" + encryptedToken);
        resp.put("jwtUrl", baseUrl + "/followup?token=" + jwtToken);
        resp.put("sessionPayload", Map.of(
                "customerName", req.getCustomerName(),
                "customerId", req.getCustomerId(),
                "orderId", req.getOrderId(),
                "date", req.getDate()
        ));
        return ResponseEntity.ok(resp);
    }

    /**
     * Rotates or adds a new JWT secret key.
     * Authorized if called by an authenticated admin or if a valid existing API key / secret is provided.
     */
    @PostMapping(value = "/api/jwt/rotate-key")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rotateKey(
            @RequestParam("newKey") String newKey,
            @RequestParam(value = "apiKey", required = false) String apiKeyParam,
            @RequestHeader(value = "X-API-KEY", required = false) String apiKeyHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String key = resolveApiKey(apiKeyHeader, authHeader, apiKeyParam);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());

        if (!isAdmin && (key == null || !externalIntegrationService.isValidApiKey(key))) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("message", "Unauthorized. Valid existing key or admin session required to rotate keys.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        if (newKey == null || newKey.trim().length() < 8) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("message", "Invalid key. New key must be at least 8 characters long.");
            return ResponseEntity.badRequest().body(err);
        }

        externalIntegrationService.rotatePrimaryKey(newKey.trim());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "Primary JWT secret key rotated successfully. Existing tokens remain valid.");
        resp.put("activeKeysCount", externalIntegrationService.getValidSecretKeys().size());
        return ResponseEntity.ok(resp);
    }

    /**
     * Status endpoint for JWT key health check.
     */
    @GetMapping(value = "/api/jwt/keys/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getKeysStatus() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("activeKeysCount", externalIntegrationService.getValidSecretKeys().size());
        resp.put("keyRotationSupported", true);
        resp.put("nonExpiringTokensSupported", true);
        return ResponseEntity.ok(resp);
    }

    private String bodyStr(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) return null;
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s) && !"undefined".equalsIgnoreCase(s)) {
                    return s;
                }
            }
        }
        return null;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.trim().isEmpty() && !"null".equalsIgnoreCase(v.trim()) && !"undefined".equalsIgnoreCase(v.trim())) {
                return v.trim();
            }
        }
        return null;
    }

    public static String extractDateFromRequest(HttpServletRequest request, String currentResolvedDate) {
        if (currentResolvedDate != null && !currentResolvedDate.trim().isEmpty()) {
            return currentResolvedDate.trim();
        }
        if (request == null) return null;

        Map<String, String[]> paramMap = request.getParameterMap();
        if (paramMap != null) {
            // 1. Check any parameter key that looks like a date key (case-insensitive, ignoring spaces/underscores/dashes)
            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                String k = entry.getKey();
                if (k == null) continue;
                String cleanKey = k.trim().toLowerCase().replaceAll("[_\\-\\s]", "");
                if (cleanKey.contains("date") || cleanKey.equals("d") || cleanKey.equals("dt")
                        || cleanKey.contains("time") || cleanKey.contains("day") || cleanKey.contains("created")) {
                    String[] vals = entry.getValue();
                    if (vals != null) {
                        for (String val : vals) {
                            if (val != null && !val.trim().isEmpty() && !"null".equalsIgnoreCase(val.trim())) {
                                return val.trim();
                            }
                        }
                    }
                }
            }
            // 2. Check if any bare parameter key or value matches a date pattern (e.g. &2026-09-15 or typo in key name like &daet=2026-09-15)
            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                String k = entry.getKey() != null ? entry.getKey().trim() : "";
                String cleanKey = k.toLowerCase();
                if (cleanKey.contains("customer") || cleanKey.contains("name") || cleanKey.contains("order") || cleanKey.equals("id") || cleanKey.contains("token")) {
                    continue;
                }
                if (k.matches("^\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}.*") || k.matches("^\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}.*")) {
                    return k;
                }
                String[] vals = entry.getValue();
                if (vals != null) {
                    for (String val : vals) {
                        if (val != null) {
                            String v = val.trim();
                            if (v.matches("^\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}.*") || v.matches("^\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}.*")) {
                                return v;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private String resolveApiKey(String headerKey, String authHeader, String queryKey) {
        if (headerKey != null && !headerKey.trim().isEmpty()) {
            return headerKey.trim();
        }
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            return authHeader.substring(7).trim();
        }
        if (queryKey != null && !queryKey.trim().isEmpty()) {
            return queryKey.trim();
        }
        return null;
    }

    private String extractBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && !forwardedProto.trim().isEmpty()) {
            scheme = forwardedProto.trim();
        }
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.trim().isEmpty()) {
            serverName = forwardedHost.trim();
            return scheme + "://" + serverName;
        }

        boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && serverPort == 80)
                || ("https".equalsIgnoreCase(scheme) && serverPort == 443);

        return scheme + "://" + serverName + (isDefaultPort ? "" : ":" + serverPort);
    }

    /**
     * API returning all administrator accounts and their assigned roles in JSON format.
     * Can be filtered optionally by id, username, or active status.
     * Accessible by external integrations, scripts, and the SV website.
     *
     * Example Endpoints:
     *   GET /api/external/admins
     *   GET /api/external/users
     *   GET /api/admins
     *   GET /api/users
     */
    @GetMapping(value = {
            "/api/external/admins",
            "/api/external/users",
            "/api/admins",
            "/api/users"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExternalAdminsJson(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "adminId", required = false) Long adminId,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "active", required = false) Boolean active) {

        Long targetId = id != null ? id : adminId;

        List<Admin> allAdmins;
        if (targetId != null) {
            allAdmins = adminService.findById(targetId).map(List::of).orElse(List.of());
        } else if (username != null && !username.trim().isEmpty()) {
            allAdmins = adminService.findByUsername(username.trim()).map(List::of).orElse(List.of());
        } else {
            allAdmins = adminService.findAll();
        }

        if (active != null) {
            allAdmins = allAdmins.stream().filter(a -> a.isActive() == active).toList();
        }

        List<AdminUserDto> adminDtos = allAdmins.stream()
                .map(AdminUserDto::fromEntity)
                .toList();

        List<RoleDto> roleDtos = roleService.findAll().stream()
                .map(r -> new RoleDto(r.getId(), r.getName(), r.isCanCreate(), r.isCanRead(), r.isCanUpdate(), r.isCanDelete(), r.getActions()))
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalCount", adminDtos.size());
        data.put("admins", adminDtos);
        data.put("users", adminDtos);
        data.put("roles", roleDtos);

        return ResponseEntity.ok(ApiResponse.success("Administrator directory and roles retrieved successfully", data));
    }

    /**
     * API returning a single administrator account and its assigned role by ID.
     */
    @GetMapping(value = {
            "/api/external/admins/{adminId:\\d+}",
            "/api/external/users/{adminId:\\d+}",
            "/api/admins/{adminId:\\d+}",
            "/api/users/{adminId:\\d+}"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<AdminUserDto>> getExternalAdminById(@PathVariable("adminId") Long adminId) {
        return adminService.findById(adminId)
                .map(AdminUserDto::fromEntity)
                .map(dto -> ResponseEntity.ok(ApiResponse.success("Administrator details retrieved successfully", dto)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Administrator not found with ID: " + adminId)));
    }

    /**
     * Customer Review Entrypoint.
     * Always returns raw JSON containing ONLY completed customer reviews (omitting pending & unfinished).
     *
     * Example Endpoints:
     *   GET /review?orderId=43356
     *   GET /review?customerId=205
     *   GET /api/review?orderId=43356
     *   GET /api/external/review?customerId=205
     */
    @RequestMapping(value = {
            "/review",
            "/api/review",
            "/api/reviews",
            "/api/external/review",
            "/api/external/reviews",
            "/api/customer-reviews"
    }, method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCustomerReviewsJson(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "customerId", required = false) String customerId,
            @RequestParam(value = "customer_id", required = false) String customerIdSnake,
            @RequestParam(value = "customerid", required = false) String customerIdLower,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "externalId", required = false) String externalId,
            @RequestParam(value = "external_id", required = false) String externalIdSnake,
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "order_id", required = false) String orderIdSnake,
            @RequestParam(value = "orderid", required = false) String orderIdLower,
            @RequestParam(value = "order", required = false) String order,
            @RequestParam(value = "o", required = false) String o,
            @RequestParam(value = "orderNumber", required = false) String orderNumber,
            @RequestParam(value = "order_number", required = false) String orderNumberSnake,
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "customer_name", required = false) String customerNameSnake,
            @RequestParam(value = "customername", required = false) String customerNameLower,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "format", required = false) String format,
            HttpServletRequest request,
            HttpServletResponse response) {

        // 1. Enforce security:
        // Accepts:
        // - Cryptographic bearer / session token: (?token=... or ?session=... or Authorization: Bearer <token> or body { "token": "..." })
        // - API Key: (?apiKey=... or X-API-KEY: ... or body { "apiKey": "..." })
        // - Active user login session in browser
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        String apiKey = firstNonEmpty(request.getHeader("X-API-KEY"), request.getParameter("apiKey"), request.getParameter("api_key"), bodyStr(body, "apiKey", "api_key", "key"));
        boolean hasValidApiKey = apiKey != null && externalIntegrationService.isValidApiKey(apiKey);

        String rawToken = firstNonEmpty(
                request.getParameter("token"),
                request.getParameter("jwt"),
                request.getParameter("jwtToken"),
                request.getParameter("session"),
                request.getParameter("t"),
                request.getHeader("X-AUTH-TOKEN"),
                bodyStr(body, "token", "jwt", "jwtToken", "accessToken", "access_token", "session", "t")
        );
        if (rawToken == null) {
            String authH = request.getHeader("Authorization");
            if (authH != null && authH.startsWith("Bearer ")) {
                rawToken = authH.substring(7).trim();
            }
        }
        ExternalReviewRequest jwtClaims = (rawToken != null) ? externalIntegrationService.parseAndVerifyReviewJwt(rawToken) : null;
        boolean hasValidJwt = jwtClaims != null;
        boolean hasValidToken = hasValidJwt || (rawToken != null && (externalIntegrationService.decryptSession(rawToken) != null || externalIntegrationService.isValidApiKey(rawToken)));

        if (!isAuthenticated && authService != null) {
            String u = firstNonEmpty(request.getParameter("username"), request.getParameter("user"), bodyStr(body, "username", "user"));
            String p = firstNonEmpty(request.getParameter("password"), request.getParameter("pass"), bodyStr(body, "password", "pass"));
            if (u != null && p != null) {
                try {
                    AuthResponse ar = authService.login(new LoginRequest(u.trim(), p), request, response);
                    if (ar != null && ar.isAuthenticated()) {
                        isAuthenticated = true;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (!isAuthenticated && !hasValidApiKey && !hasValidToken) {
            String acceptHeader = request.getHeader("Accept");
            boolean isBrowser = acceptHeader != null && acceptHeader.contains("text/html");
            if (isBrowser) {
                String fullUri = request.getRequestURI();
                String queryString = request.getQueryString();
                String targetUrl = (queryString != null && !queryString.trim().isEmpty()) ? fullUri + "?" + queryString : fullUri;
                log.info("Unauthenticated browser access on {}. Prompting login.", targetUrl);
                request.getSession(true).setAttribute("TARGET_AFTER_LOGIN", targetUrl);
                try {
                    jakarta.servlet.http.Cookie c = new jakarta.servlet.http.Cookie("TARGET_AFTER_LOGIN", java.net.URLEncoder.encode(targetUrl, java.nio.charset.StandardCharsets.UTF_8));
                    c.setPath("/");
                    c.setMaxAge(600);
                    response.addCookie(c);
                } catch (Exception ignored) {}
                try {
                    response.sendRedirect("/login?redirect=" + java.net.URLEncoder.encode(targetUrl, java.nio.charset.StandardCharsets.UTF_8));
                    return null;
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
            } else {
                Map<String, Object> unauth = new LinkedHashMap<>();
                unauth.put("error", "Unauthorized");
                unauth.put("message", "Full authentication required. Please provide a valid JWT via 'Authorization: Bearer <jwt>' header, '?token=<jwt>', or 'X-API-KEY'.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(unauth);
            }
        }

        String resolvedOrderId = firstNonEmpty(orderId, orderIdSnake, orderIdLower, order, o, orderNumber, orderNumberSnake, bodyStr(body, "orderId", "order_id", "orderid", "order", "orderNumber", "order_number"));
        String resolvedCustId = firstNonEmpty(customerId, customerIdSnake, customerIdLower, externalId, externalIdSnake, bodyStr(body, "customerId", "customer_id", "customerid", "externalId", "external_id", "id"));
        String resolvedCustName = firstNonEmpty(customerName, customerNameSnake, customerNameLower, name, bodyStr(body, "customerName", "customer_name", "customername", "name"));

        // Auto-extract customerId or orderId from verified JWT payload if not specified in request
        if (jwtClaims != null) {
            if (resolvedCustId == null && jwtClaims.getCustomerId() != null) {
                resolvedCustId = jwtClaims.getCustomerId();
            }
            if (resolvedOrderId == null && jwtClaims.getOrderId() != null) {
                resolvedOrderId = jwtClaims.getOrderId();
            }
            if (resolvedCustName == null && jwtClaims.getCustomerName() != null && !"Customer".equalsIgnoreCase(jwtClaims.getCustomerName())) {
                resolvedCustName = jwtClaims.getCustomerName();
            }
        }

        String generalQuery = null;
        if (resolvedCustId == null && resolvedOrderId == null && resolvedCustName == null) {
            generalQuery = firstNonEmpty(id, search, q);
            if (generalQuery != null) {
                if (generalQuery.matches("^\\d+$")) {
                    resolvedOrderId = generalQuery;
                } else {
                    resolvedCustName = generalQuery;
                }
            }
        }

        if (body != null && (body.containsKey("notes") || body.containsKey("satisfaction"))) {
            Map<String, Object> submitRes = externalIntegrationService.submitReview(
                    resolvedOrderId,
                    bodyStr(body, "notes", "note"),
                    bodyStr(body, "satisfaction"),
                    bodyStr(body, "photoUrl", "photo")
            );
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(submitRes);
        }

        Map<String, Object> data = externalIntegrationService.getCustomerReviews(
                resolvedCustId, resolvedOrderId, resolvedCustName);

        data.remove("message");
        data.remove("found");
        data.remove("reviewed");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(data);
    }

    /**
     * Customer Review by Order ID or Customer ID Path Variable.
     * Always returns raw JSON containing ONLY completed customer reviews.
     * e.g. GET /review/43356 or GET /api/review/43356
     */
    @RequestMapping(value = {
            "/review/{id:\\d+}",
            "/api/review/{id:\\d+}",
            "/api/reviews/{id:\\d+}",
            "/api/external/review/{id:\\d+}",
            "/api/external/reviews/{id:\\d+}"
    }, method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCustomerReviewsById(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "format", required = false) String format,
            HttpServletRequest request,
            HttpServletResponse response) {

        // 1. Enforce security:
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        String apiKey = firstNonEmpty(request.getHeader("X-API-KEY"), request.getParameter("apiKey"), request.getParameter("api_key"));
        boolean hasValidApiKey = apiKey != null && externalIntegrationService.isValidApiKey(apiKey);

        String rawToken = firstNonEmpty(
                request.getParameter("token"),
                request.getParameter("jwt"),
                request.getParameter("jwtToken"),
                request.getParameter("session"),
                request.getParameter("t"),
                request.getHeader("X-AUTH-TOKEN")
        );
        if (rawToken == null) {
            String authH = request.getHeader("Authorization");
            if (authH != null && authH.startsWith("Bearer ")) {
                rawToken = authH.substring(7).trim();
            }
        }
        ExternalReviewRequest jwtClaims = (rawToken != null) ? externalIntegrationService.parseAndVerifyReviewJwt(rawToken) : null;
        boolean hasValidJwt = jwtClaims != null;
        boolean hasValidToken = hasValidJwt || (rawToken != null && (externalIntegrationService.decryptSession(rawToken) != null || externalIntegrationService.isValidApiKey(rawToken)));

        if (!isAuthenticated && authService != null) {
            String u = firstNonEmpty(request.getParameter("username"), request.getParameter("user"));
            String p = firstNonEmpty(request.getParameter("password"), request.getParameter("pass"));
            if (u != null && p != null) {
                try {
                    AuthResponse ar = authService.login(new LoginRequest(u.trim(), p), request, response);
                    if (ar != null && ar.isAuthenticated()) {
                        isAuthenticated = true;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (!isAuthenticated && !hasValidApiKey && !hasValidToken) {
            String acceptHeader = request.getHeader("Accept");
            boolean isBrowser = acceptHeader != null && acceptHeader.contains("text/html");
            if (isBrowser) {
                String fullUri = request.getRequestURI();
                String queryString = request.getQueryString();
                String targetUrl = (queryString != null && !queryString.trim().isEmpty()) ? fullUri + "?" + queryString : fullUri;
                log.info("Unauthenticated browser access on {}. Prompting login.", targetUrl);
                request.getSession(true).setAttribute("TARGET_AFTER_LOGIN", targetUrl);
                try {
                    jakarta.servlet.http.Cookie c = new jakarta.servlet.http.Cookie("TARGET_AFTER_LOGIN", java.net.URLEncoder.encode(targetUrl, java.nio.charset.StandardCharsets.UTF_8));
                    c.setPath("/");
                    c.setMaxAge(600);
                    response.addCookie(c);
                } catch (Exception ignored) {}
                try {
                    response.sendRedirect("/login?redirect=" + java.net.URLEncoder.encode(targetUrl, java.nio.charset.StandardCharsets.UTF_8));
                    return null;
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
            } else {
                Map<String, Object> unauth = new LinkedHashMap<>();
                unauth.put("error", "Unauthorized");
                unauth.put("message", "Full authentication required. Please provide a valid ?token=..., X-API-KEY header, or log in.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(unauth);
            }
        }

        if (body != null && (body.containsKey("notes") || body.containsKey("satisfaction"))) {
            Map<String, Object> submitRes = externalIntegrationService.submitReview(
                    id,
                    bodyStr(body, "notes", "note"),
                    bodyStr(body, "satisfaction"),
                    bodyStr(body, "photoUrl", "photo")
            );
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(submitRes);
        }

        // Check as Order ID first (since supervisor searches by Order ID), then fallback to Customer ID
        Map<String, Object> data = externalIntegrationService.getCustomerReviews(null, id, null);
        if (Boolean.FALSE.equals(data.get("found"))) {
            data = externalIntegrationService.getCustomerReviews(id, null, null);
        }

        data.remove("message");
        data.remove("found");
        data.remove("reviewed");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(data);
    }

    /**
     * Clean URL Endpoint for Customer Reviews by Order ID.
     * Accessible via:
     *   GET /followup/review/{orderId}
     *   GET /followups/review/{orderId}
     *   GET /admin/followup/review/{orderId}
     *   GET /admin/followups/review/{orderId}
     *   GET /admin/review/{orderId}
     *
     * Dual-Mode Behavior:
     * - Browser: Renders the Twekl follow-ups & review screen directly with the clean URL.
     * - API / JSON: Returns JSON data of completed reviews.
     */
    @GetMapping(value = {
            "/followup/review/{orderId:\\d+}",
            "/followups/review/{orderId:\\d+}",
            "/admin/followup/review/{orderId:\\d+}",
            "/admin/followups/review/{orderId:\\d+}",
            "/admin/review/{orderId:\\d+}"
    }, produces = {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE, "*/*"})
    public Object showCleanReviewByOrderId(
            @PathVariable("orderId") String orderId,
            @RequestParam(value = "format", required = false) String format,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        String acceptHeader = request.getHeader("Accept");
        String uri = request.getRequestURI();
        boolean isApiUri = uri != null && uri.startsWith("/api/");
        boolean isExplicitJson = "json".equalsIgnoreCase(format) || "raw".equalsIgnoreCase(format);
        boolean isBrowser = acceptHeader != null && acceptHeader.contains("text/html");

        // If JSON API request:
        if (!isBrowser || isExplicitJson) {
            String apiKey = firstNonEmpty(request.getHeader("X-API-KEY"), request.getParameter("apiKey"), request.getParameter("api_key"));
            boolean hasValidApiKey = apiKey != null && externalIntegrationService.isValidApiKey(apiKey);
            Authentication authApi = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthApi = authApi != null && authApi.isAuthenticated() && !"anonymousUser".equals(authApi.getPrincipal());
            if (!isAuthApi && !hasValidApiKey) {
                Map<String, Object> unauth = new LinkedHashMap<>();
                unauth.put("error", "Unauthorized");
                unauth.put("message", "Full authentication required. Please log in or provide a valid X-API-KEY.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(unauth);
            }

            Map<String, Object> data = externalIntegrationService.getCustomerReviews(null, orderId, null);
            if (Boolean.FALSE.equals(data.get("found"))) {
                data = externalIntegrationService.getCustomerReviews(orderId, null, null);
            }
            data.remove("message");
            data.remove("found");
            data.remove("reviewed");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(data);
        }

        // Browser navigation:
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());

        String targetUrl = uri;
        if (targetUrl == null || targetUrl.trim().isEmpty()) {
            targetUrl = "/followup/review/" + orderId;
        }

        if (!isAuthenticated) {
            log.info("Unauthenticated user accessing review URL {}. Prompting login and saving target.", targetUrl);
            request.getSession(true).setAttribute("TARGET_AFTER_LOGIN", targetUrl);
            try {
                jakarta.servlet.http.Cookie c = new jakarta.servlet.http.Cookie("TARGET_AFTER_LOGIN", java.net.URLEncoder.encode(targetUrl, java.nio.charset.StandardCharsets.UTF_8));
                c.setPath("/");
                c.setMaxAge(600);
                response.addCookie(c);
            } catch (Exception ignored) {}
            return "redirect:/login?redirect=" + java.net.URLEncoder.encode(targetUrl, java.nio.charset.StandardCharsets.UTF_8);
        }

        boolean canRead = adminService.getCurrentAdmin()
                .map(a -> a.hasAction("CUSTOMER_VIEW_LIST"))
                .orElse(false);

        if (!canRead) {
            log.warn("Authenticated user lacks CUSTOMER_VIEW_LIST permission on {}. Forwarding to dashboard.", targetUrl);
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("activeTab", "followups");
        model.addAttribute("presets", timeFilterService.findAll());
        return "administration/followup/followups";
    }

    /**
     * Default Review Redirect for bare routes without Order ID path variable.
     */
    @GetMapping(value = {
            "/followup/review",
            "/followups/review",
            "/admin/followup/review",
            "/admin/review"
    })
    public Object showCleanReviewDefault(
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "order_id", required = false) String orderIdSnake,
            @RequestParam(value = "id", required = false) String id) {

        String targetOrderId = firstNonEmpty(orderId, orderIdSnake, id);
        if (targetOrderId != null && targetOrderId.matches("^\\d+$")) {
            return "redirect:/followup/review/" + targetOrderId;
        }
        return "redirect:/admin/followups?status=DONE";
    }

    /**
     * Clean 3-Parameter Review API:
     * Returns or submits:
     * - "order": order ID / order number
     * - "customerId": customer ID
     * - "lastReview": review status or details
     *
     * Example Endpoints:
     *   GET /api/last-review?order=4399
     *   GET /api/last-review/4399
     *   GET /api/last-review?customerId=201
     *   POST /api/last-review  (with body: { "order": "4399" })
     *   POST /api/last-review  (with body: { "order": "4399", "customerId": "201", "lastReview": "SATISFIED" })
     */
    @RequestMapping(value = {
            "/api/last-review",
            "/api/last-reviews",
            "/api/latest-review",
            "/api/latest-reviews",
            "/api/last-review/{id}",
            "/api/latest-review/{id}",
            "/api/review/last",
            "/api/review/latest"
    }, method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public ResponseEntity<?> getLastReviewApi(
            @PathVariable(value = "id", required = false) String idPath,
            @RequestParam(value = "order", required = false) String orderParam,
            @RequestParam(value = "orderId", required = false) String orderIdParam,
            @RequestParam(value = "order_id", required = false) String orderIdSnake,
            @RequestParam(value = "customerId", required = false) String customerIdParam,
            @RequestParam(value = "customer_id", required = false) String customerIdSnake,
            @RequestParam(value = "format", required = false) String formatParam,
            @RequestParam(value = "simple", required = false) Boolean simpleParam,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {

        boolean isSimple = Boolean.TRUE.equals(simpleParam)
                || "simple".equalsIgnoreCase(formatParam)
                || "string".equalsIgnoreCase(formatParam);

        // Security check: require authenticated admin session, valid API key, or Bearer JWT token
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        String apiKey = firstNonEmpty(request.getHeader("X-API-KEY"), request.getParameter("apiKey"), request.getParameter("api_key"), bodyStr(body, "apiKey", "api_key", "key"));
        boolean hasValidApiKey = apiKey != null && externalIntegrationService.isValidApiKey(apiKey);

        String rawToken = firstNonEmpty(
                request.getParameter("token"),
                request.getParameter("jwt"),
                request.getParameter("jwtToken"),
                request.getHeader("X-AUTH-TOKEN"),
                bodyStr(body, "token", "jwt", "jwtToken", "accessToken")
        );
        if (rawToken == null) {
            String authH = request.getHeader("Authorization");
            if (authH != null && authH.startsWith("Bearer ")) {
                rawToken = authH.substring(7).trim();
            }
        }
        boolean hasValidToken = (rawToken != null && (externalIntegrationService.decryptSession(rawToken) != null || externalIntegrationService.isValidApiKey(rawToken)));

        if (!isAuthenticated && !hasValidApiKey && !hasValidToken) {
            Map<String, Object> unauth = new LinkedHashMap<>();
            unauth.put("status", 401);
            unauth.put("error", "Unauthorized");
            unauth.put("message", "Authentication required. Please provide a valid Bearer token in 'Authorization: Bearer <token>' header or '?token=<token>'");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(unauth);
        }

        String targetOrder = firstNonEmpty(
                idPath, orderParam, orderIdParam, orderIdSnake,
                bodyStr(body, "order", "orderId", "order_id", "orderNumber", "order_number")
        );

        String targetCustomer = firstNonEmpty(
                customerIdParam, customerIdSnake,
                bodyStr(body, "customerId", "customer_id", "customer")
        );

        // If POST request includes a lastReview to submit/save:
        if (body != null && (body.containsKey("lastReview") || body.containsKey("last_review") || body.containsKey("satisfaction"))) {
            Object revObj = body.get("lastReview");
            if (revObj == null) revObj = body.get("last_review");

            String sat = null;
            String notes = null;
            if (revObj instanceof String strRev) {
                sat = strRev;
            } else if (revObj instanceof Map<?, ?> revMap) {
                if (revMap.get("satisfaction") != null) sat = revMap.get("satisfaction").toString();
                if (revMap.get("notes") != null) notes = revMap.get("notes").toString();
            }
            if (sat == null && body.get("satisfaction") != null) {
                sat = body.get("satisfaction").toString();
            }
            if (notes == null && body.get("notes") != null) {
                notes = body.get("notes").toString();
            }

            if (targetOrder != null && sat != null) {
                externalIntegrationService.submitReview(targetOrder, notes, sat, null);
            }
        }

        // Single order or customer lookup
        if (targetOrder != null || targetCustomer != null) {
            Map<String, Object> result = externalIntegrationService.getLastReview(targetOrder, targetCustomer, isSimple);
            if (result == null || (result.containsKey("status") && Integer.valueOf(404).equals(result.get("status")))) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result != null ? result : Map.of("status", 404, "message", "Order or customer not found"));
            }
            return ResponseEntity.ok(result);
        }

        // No order specified: return list of all reviewed orders
        List<Map<String, Object>> allList = externalIntegrationService.getAllLastReviews(isSimple);
        return ResponseEntity.ok(allList);
    }
}
