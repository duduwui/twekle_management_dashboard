package com.twekl.dashboard.service.administrative;

import com.twekl.dashboard.dto.administrative.LoginRequest;
import com.twekl.dashboard.model.administrative.Admin;
import com.twekl.dashboard.repository.administrative.AdminRepository;
import com.twekl.dashboard.response.administrative.AuthResponse;
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
 */
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AdminRepository adminRepository;

    private SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthService() {
    }

    @Autowired
    public AuthService(AuthenticationManager authenticationManager,
                       AdminRepository adminRepository) {
        this.authenticationManager = authenticationManager;
        this.adminRepository = adminRepository;
        this.securityContextRepository = new HttpSessionSecurityContextRepository();
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        Admin admin = adminRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new RuntimeException("Authenticated admin profile not found"));

        return AuthResponse.fromAdmin(admin);
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
