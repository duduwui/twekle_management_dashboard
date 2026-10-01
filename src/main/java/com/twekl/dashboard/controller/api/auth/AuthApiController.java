package com.twekl.dashboard.controller.api.auth;

import com.twekl.dashboard.dto.AuthResponse;
import com.twekl.dashboard.dto.LoginRequest;
import com.twekl.dashboard.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication", description = "User authentication, session verification, and logout operations")
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;

    @Autowired
    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Authenticate user", description = "Validates credentials and establishes an authenticated HTTP session")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        AuthResponse response = authService.authenticate(request, httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout user", description = "Clears security context and invalidates session")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @Operation(summary = "Get current authenticated user info", description = "Returns the session context, username, and role permissions")
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        boolean isSuperAdmin = roles.contains("ROLE_SUPER_ADMIN");
        boolean isAdmin = roles.contains("ROLE_ADMIN") || isSuperAdmin;
        boolean canCreateRoles = roles.contains("PERMISSION_CREATE_ROLES") || isSuperAdmin;
        boolean canManageUsers = roles.contains("PERMISSION_MANAGE_USERS") || isSuperAdmin;

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", auth.getName(),
                "role", isSuperAdmin ? "SUPER_ADMIN" : (isAdmin ? "ADMIN" : "USER"),
                "isSuperAdmin", isSuperAdmin,
                "canCreateRoles", canCreateRoles,
                "canManageUsers", canManageUsers,
                "authorities", roles
        ));
    }
}
