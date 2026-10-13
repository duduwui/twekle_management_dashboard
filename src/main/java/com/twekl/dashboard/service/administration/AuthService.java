package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.LoginRequest;
import com.twekl.dashboard.response.administration.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Service contract for administrator authentication, session handling, and identity resolution.
 */
public interface AuthService {

    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    void logout(HttpServletRequest httpRequest);

    AuthResponse getCurrentUser(HttpServletRequest httpRequest);
}
