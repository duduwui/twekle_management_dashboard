package com.twekl.dashboard.security.administration;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter()
                .write(
                        "<!DOCTYPE html><html lang=\"en\"><head><title>Access"
                            + " denied</title></head><body><h1>Access denied</h1><p>Your account"
                            + " lacks permission, or the form has expired. Reload the page before"
                            + " trying again.</p></body></html>");
    }
}
