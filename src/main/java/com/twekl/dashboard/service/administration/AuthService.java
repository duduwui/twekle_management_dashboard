package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.LoginRequest;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.repository.administration.AdminRepository;
import com.twekl.dashboard.response.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for administrator authentication, session handling, and identity resolution.
 * Consolidated single service file with final autowired dependencies.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AdminRepository adminRepository;
    private final SecurityContextRepository securityContextRepository;
    private final ExternalIntegrationService externalIntegrationService;

    @Autowired
    public AuthService(final AuthenticationManager authenticationManager,
                       final AdminRepository adminRepository,
                       @org.springframework.context.annotation.Lazy final ExternalIntegrationService externalIntegrationService) {
        this.authenticationManager = authenticationManager;
        this.adminRepository = adminRepository;
        this.externalIntegrationService = externalIntegrationService;
        this.securityContextRepository = new HttpSessionSecurityContextRepository();
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
        );

        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        Admin admin = adminRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new RuntimeException("Authenticated admin profile not found"));

        AuthResponse authResponse = AuthResponse.fromAdmin(admin);

        long jwtValiditySeconds = 86400L * 7; // 7 days in seconds
        if (externalIntegrationService != null) {
            String jwt = externalIntegrationService.generateUserJwt(admin.getUsername(), authResponse.getRole(), jwtValiditySeconds);
            authResponse.setToken(jwt);
            authResponse.setTokenType("Bearer");
            authResponse.setExpiresIn(jwtValiditySeconds);
        }

        HttpSession session = httpRequest.getSession(true);
        if (session != null) {
            session.setMaxInactiveInterval(30 * 24 * 60 * 60); // 30 days in seconds
            String target = (String) session.getAttribute("TARGET_AFTER_LOGIN");
            if (target != null && !target.trim().isEmpty()) {
                session.removeAttribute("TARGET_AFTER_LOGIN");
                authResponse.setRedirectUrl(target.trim());
            }
        }

        return authResponse;
    }

    public void logout(HttpServletRequest httpRequest) {
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public AuthResponse getCurrentUser(HttpServletRequest httpRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        String username = auth.getName();
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isEmpty()) {
            return null;
        }

        return AuthResponse.fromAdmin(adminOpt.get());
    }
}
