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
    public String showLoginForm() {
        if (isAuthenticated()) {
            return "redirect:/admin/dashboard";
        }
        return "administration/login";
    }

    // =========================================================
    // REST AUTH ENDPOINTS
    // =========================================================

    @PostMapping("/admin/auth/login")
    @ResponseBody
    public ResponseEntity<ApiResponse<AuthResponse>> loginAdmin(@Valid @RequestBody LoginRequest request,
                                                                HttpServletRequest httpRequest,
                                                                HttpServletResponse httpResponse) {
        AuthResponse response = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
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
