package com.twekl.dashboard.service;

import com.twekl.dashboard.dto.AuthResponse;
import com.twekl.dashboard.dto.LoginRequest;
import com.twekl.dashboard.model.Admin;
import com.twekl.dashboard.repository.AdminRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AdminRepository adminRepository;

    @Autowired
    public AuthService(AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository,
                       AdminRepository adminRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.adminRepository = adminRepository;
    }

    @Transactional
    public AuthResponse authenticate(LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            throw new IllegalArgumentException("Username and password are required");
        }

        String username = loginRequest.getUsername().trim();

        try {
            // 1. Authenticate with Spring Security AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );

            // 2. Establish Spring SecurityContext
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            // 3. Retrieve admin details for client response
            Optional<Admin> adminOpt = adminRepository.findByUsername(username);
            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                String roleName = admin.isSuperAdmin() ? "SUPER_ADMIN" : "ADMIN";
                return new AuthResponse(
                        true,
                        admin.getUsername(),
                        roleName,
                        admin.isSuperAdmin(),
                        admin.isCanCreateRoles(),
                        admin.isCanManageUsers(),
                        "Authentication successful"
                );
            }

            // Fallback for regular app user login
            return new AuthResponse(
                    true,
                    username,
                    "USER",
                    false,
                    false,
                    false,
                    "User authentication successful"
            );
        } catch (BadCredentialsException e) {
            throw new SecurityException("Invalid username or password");
        } catch (DisabledException e) {
            throw new SecurityException("Account is disabled. Please contact an administrator.");
        }
    }
}
