package com.twekl.dashboard.administration.service.admin;

import com.twekl.dashboard.administration.repository.admin.AdminRepository;
import com.twekl.dashboard.administration.dto.admin.LoginRequest;
import com.twekl.dashboard.administration.response.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AdminRepository adminRepository;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       AdminRepository adminRepository,
                       SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.adminRepository = adminRepository;
        this.securityContextRepository = securityContextRepository;
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword());

        Authentication auth = authenticationManager.authenticate(authToken);
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);

        if (httpRequest != null) {
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
            if (httpResponse != null && securityContextRepository != null) {
                securityContextRepository.saveContext(sc, httpRequest, httpResponse);
            }
        }

        return getCurrentUser(httpRequest);
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        return login(request, httpRequest, null);
    }

    public void logout(HttpServletRequest httpRequest) {
        SecurityContextHolder.clearContext();
        if (httpRequest != null) {
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
    }

    public AuthResponse getCurrentUser() {
        return getCurrentUser(null);
    }

    public AuthResponse getCurrentUser(HttpServletRequest request) {
        Authentication auth = null;
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object scObj = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                if (scObj instanceof SecurityContext) {
                    auth = ((SecurityContext) scObj).getAuthentication();
                }
            }
        }
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            Authentication holderAuth = SecurityContextHolder.getContext().getAuthentication();
            if (holderAuth != null && holderAuth.isAuthenticated() && !"anonymousUser".equals(holderAuth.getPrincipal())) {
                if (request == null || request.getSession(false) != null) {
                    auth = holderAuth;
                }
            }
        }

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return AuthResponse.failed("Not authenticated");
        }

        String username = auth.getName();
        return adminRepository.findByUsername(username)
                .map(AuthResponse::fromAdmin)
                .orElseGet(() -> AuthResponse.success(username, "ADMIN", false));
    }
}

