package com.twekl.dashboard.exception.administration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class, org.springframework.web.servlet.NoHandlerFoundException.class})
    public ModelAndView missing(Exception error) {
        return page(HttpStatus.NOT_FOUND, "The requested page or record was not found.");
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        BadRequestException.class,
        MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ModelAndView invalid(Exception error) {
        return page(
                HttpStatus.BAD_REQUEST,
                "The submitted values are invalid. Return to the form and correct them.");
    }

    @ExceptionHandler({AccessDeniedException.class, SecurityException.class})
    public ModelAndView denied(Exception error) {
        return page(HttpStatus.FORBIDDEN, "You do not have permission for this action.");
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ModelAndView conflict(Exception error) {
        return page(
                HttpStatus.CONFLICT,
                "This record conflicts with existing data or is still in use.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView failed(Exception error) {
        log.error("Portal request failed", error);
        return page(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The request could not be completed. Please try again later.");
    }

    private ModelAndView page(HttpStatus status, String message) {
        ModelAndView result = new ModelAndView("administration/error");
        result.setStatus(status);
        result.addObject("status", status.value());
        result.addObject("message", message);
        return result;
    }
}
