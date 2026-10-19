package com.twekl.dashboard.exception;

import com.twekl.dashboard.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * ClientAbortException / BrokenPipe — the browser closed the connection before
     * the response was fully written (e.g. tab closed, navigation away from page).
     * This is NOT an application error. We log at DEBUG and do nothing — there is
     * no live socket to send a response to anyway.
     */
    @ExceptionHandler({
        org.apache.catalina.connector.ClientAbortException.class,
        AsyncRequestNotUsableException.class
    })
    public void handleClientAbort(Exception ex, HttpServletRequest request) {
        log.debug("Client closed connection for [{}]: {}", request.getRequestURI(), ex.getMessage());
        // No response possible — client is gone.
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                fieldErrors.put(error.getObjectName(), error.getDefaultMessage())
        );

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Invalid input parameters provided",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    private boolean isHtmlRequest(HttpServletRequest request) {
        if (request == null) return false;
        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        String uri = request.getRequestURI();
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) return false;
        if (uri != null && (uri.contains("/paged") || uri.contains("/data") || uri.contains("/stats") || uri.endsWith(".json"))) return false;
        return accept != null && accept.contains("text/html");
    }

    private Object buildErrorResponse(HttpStatus status, String error, String message, HttpServletRequest request) {
        if (isHtmlRequest(request)) {
            org.springframework.web.servlet.ModelAndView mav = new org.springframework.web.servlet.ModelAndView("administration/error");
            mav.setStatus(status);
            mav.addObject("status", status.value());
            mav.addObject("error", error);
            mav.addObject("message", message);
            mav.addObject("path", request.getRequestURI());
            return mav;
        }
        ErrorResponse errorResponse = new ErrorResponse(status.value(), error, message, request.getRequestURI());
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public Object handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler({
        AuthenticationException.class,
        AuthenticationCredentialsNotFoundException.class
    })
    public Object handleAuthenticationException(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
    }

    @ExceptionHandler({
        SecurityException.class,
        AccessDeniedException.class
    })
    public Object handleSecurityException(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden",
                ex.getMessage() != null ? ex.getMessage() : "Access denied", request);
    }

    @ExceptionHandler({NoResourceFoundException.class, org.springframework.web.servlet.NoHandlerFoundException.class})
    public Object handleNotFound(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found",
                "The requested resource was not found: " + request.getRequestURI(), request);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request) {
        // Check if this is a broken-pipe / client-abort wrapped in another exception
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof IOException && cause.getMessage() != null
                    && (cause.getMessage().contains("Broken pipe") || cause.getMessage().contains("Connection reset"))) {
                log.warn("Client disconnected mid-response for [{}]", request.getRequestURI());
                // Cannot write response — return nothing (socket is closed)
                return null;
            }
            cause = cause.getCause();
        }
        log.error("Unhandled exception processing request to {}", request.getRequestURI(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected internal error occurred", request);
    }
}
