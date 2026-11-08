package com.twekl.dashboard.security.administration;

import com.twekl.dashboard.dto.administration.LoginRequest;
import com.twekl.dashboard.response.ApiResponse;
import com.twekl.dashboard.response.AuthResponse;
import com.twekl.dashboard.service.administration.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Admin Authentication and Profile Session.
 * Handles SSR login view rendering and REST authentication endpoints.
 */
@Controller
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(final AuthService authService) {
        this.authService = authService;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    // =========================================================
    // SSR LOGIN VIEW
    // =========================================================

    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(value = "redirect", required = false) String redirect,
            HttpServletRequest request) {
        if (redirect != null && !redirect.trim().isEmpty()) {
            request.getSession(true).setAttribute("TARGET_AFTER_LOGIN", redirect.trim());
        }
        if (isAuthenticated()) {
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            if (session != null) {
                String target = (String) session.getAttribute("TARGET_AFTER_LOGIN");
                if (target != null && !target.trim().isEmpty()) {
                    session.removeAttribute("TARGET_AFTER_LOGIN");
                    return "redirect:" + target;
                }
            }
            return "redirect:/admin/dashboard";
        }
        return "administration/login";
    }

    // =========================================================
    // REST AUTH ENDPOINTS
    // =========================================================

    @PostMapping(value = {
            "/admin/auth/login",
            "/api/auth/login",
            "/api/login"
    }, consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<AuthResponse>> loginAdminJson(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty() || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username and password are required"));
        }
        return processAdminLogin(request.getUsername().trim(), request.getPassword(), httpRequest, httpResponse);
    }

    @PostMapping(value = {
            "/admin/auth/login",
            "/api/auth/login",
            "/api/login"
    }, consumes = org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<AuthResponse>> loginAdminForm(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (username == null || username.trim().isEmpty() || password == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username and password are required"));
        }
        return processAdminLogin(username.trim(), password, httpRequest, httpResponse);
    }

    private ResponseEntity<ApiResponse<AuthResponse>> processAdminLogin(
            String username,
            String password,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            LoginRequest req = new LoginRequest(username, password);
            AuthResponse response = authService.login(req, httpRequest, httpResponse);
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid username or password"));
        }
    }

    /**
     * Dedicated Token Endpoint for API & Script Integrations.
     * Takes username & password (via JSON body or URL-encoded form data),
     * and returns a standard signed JWT token with expiry.
     *
     * Example Usage:
     *   curl -X POST http://192.168.1.7:8085/api/auth/token \
     *        -H "Content-Type: application/json" \
     *        -d '{"username":"admin","password":"your_password"}'
     */
    @PostMapping(value = {
            "/api/auth/token",
            "/api/token",
            "/oauth/token"
    }, consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> getApiTokenJson(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty() || request.getPassword() == null) {
            java.util.Map<String, Object> err = new java.util.LinkedHashMap<>();
            err.put("status", 400);
            err.put("message", "Bad Request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
        return processApiToken(request.getUsername().trim(), request.getPassword(), httpRequest, httpResponse);
    }

    @RequestMapping(value = {
            "/api/auth/token",
            "/api/token",
            "/oauth/token"
    }, method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> getApiTokenParams(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (username == null || username.trim().isEmpty() || password == null) {
            java.util.Map<String, Object> err = new java.util.LinkedHashMap<>();
            err.put("status", 400);
            err.put("message", "Bad Request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
        return processApiToken(username.trim(), password, httpRequest, httpResponse);
    }

    private ResponseEntity<java.util.Map<String, Object>> processApiToken(
            String username,
            String password,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            LoginRequest req = new LoginRequest(username, password);
            AuthResponse authResponse = authService.login(req, httpRequest, httpResponse);

            java.util.Map<String, Object> tokenPayload = new java.util.LinkedHashMap<>();
            tokenPayload.put("status", 200);
            tokenPayload.put("token", authResponse.getToken());
            tokenPayload.put("expiresIn", authResponse.getExpiresIn());
            tokenPayload.put("message", "OK");
            return ResponseEntity.ok(tokenPayload);
        } catch (Exception e) {
            java.util.Map<String, Object> err = new java.util.LinkedHashMap<>();
            err.put("status", 401);
            err.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }
    }

    @PostMapping("/admin/auth/logout")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> logoutAdmin(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @GetMapping("/admin/auth/me")
    @ResponseBody
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentAuthenticatedAdmin(HttpServletRequest httpRequest) {
        AuthResponse response = authService.getCurrentUser(httpRequest);
        if (response == null || !response.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
