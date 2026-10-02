package com.twekl.dashboard.modules.auth.service;

import com.twekl.dashboard.modules.admin.entity.Admin;
import com.twekl.dashboard.modules.admin.repository.AdminRepository;
import com.twekl.dashboard.modules.auth.dto.AuthResponse;
import com.twekl.dashboard.modules.auth.dto.LoginRequest;
import com.twekl.dashboard.modules.user.entity.AppUser;
import com.twekl.dashboard.modules.user.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AdminRepository adminRepository;
    private final AppUserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       AdminRepository adminRepository,
                       AppUserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword());

        Authentication auth = authenticationManager.authenticate(authToken);
        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(auth);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

        return getCurrentUser();
    }

    public void logout(HttpServletRequest httpRequest) {
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public AuthResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("No authenticated user session");
        }

        String username = auth.getName();
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            return new AuthResponse(admin.getUsername(), "ADMIN", admin.isSuperAdmin(), admin.isCanCreateRoles(), admin.isCanManageUsers());
        }

        Optional<AppUser> userOpt = userRepository.findByUsernameEn(username);
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            return new AuthResponse(user.getUsernameEn(), "USER", false, false, false);
        }

        return new AuthResponse(username, "USER", false, false, false);
    }
}
