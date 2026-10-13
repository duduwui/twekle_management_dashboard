package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.LoginRequest;
import com.twekl.dashboard.response.administration.ApiResponse;
import com.twekl.dashboard.response.administration.AuthResponse;
import com.twekl.dashboard.service.administration.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller for Admin Authentication and Profile Session.
 * Dedicated auth domain controller.
 */
@RestController
@RequestMapping("/admin/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    public AuthController() {
    }

    @Autowired
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
        if (response == null || !response.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
