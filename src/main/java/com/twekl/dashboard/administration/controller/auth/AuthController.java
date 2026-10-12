package com.twekl.dashboard.administration.controller.auth;

import com.twekl.dashboard.administration.dto.admin.LoginRequest;
import com.twekl.dashboard.administration.response.ApiResponse;
import com.twekl.dashboard.administration.response.AuthResponse;
import com.twekl.dashboard.administration.service.admin.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Admin Authentication and Profile Session.
 * Standardized under /admin/auth route.
 */
@RestController
@RequestMapping({"/admin/auth", "/internal/auth"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginAdmin(@Valid @RequestBody LoginRequest request,
                                                               HttpServletRequest httpRequest,
                                                               HttpServletResponse httpResponse) {
        AuthResponse response = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logoutAdmin(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentAuthenticatedAdmin(HttpServletRequest httpRequest) {
        AuthResponse response = authService.getCurrentUser(httpRequest);
        if (!response.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
