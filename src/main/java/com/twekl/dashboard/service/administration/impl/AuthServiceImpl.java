package com.twekl.dashboard.service.administration.impl;

import com.twekl.dashboard.dto.administration.LoginRequest;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.repository.administration.AdminRepository;
import com.twekl.dashboard.response.administration.AuthResponse;
import com.twekl.dashboard.service.administration.AuthService;
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
 * Implementation of AuthService for administrator authentication, session handling, and identity resolution.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AdminRepository adminRepository;
    private final SecurityContextRepository securityContextRepository;

    @Autowired
    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           AdminRepository adminRepository) {
        this.authenticationManager = authenticationManager;
        this.adminRepository = adminRepository;
        this.securityContextRepository = new HttpSessionSecurityContextRepository();
    }

    @Override
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

        return AuthResponse.fromAdmin(admin);
    }

    @Override
    public void logout(HttpServletRequest httpRequest) {
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
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
