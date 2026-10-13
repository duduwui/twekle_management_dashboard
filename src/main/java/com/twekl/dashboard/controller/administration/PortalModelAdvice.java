package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.security.administration.PortalAccess;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PortalModelAdvice {
    private final PortalAccess access;

    public PortalModelAdvice(PortalAccess access) {
        this.access = access;
    }

    @ModelAttribute("currentUser")
    public Admin currentUser() {
        return access.getCurrent();
    }
}
