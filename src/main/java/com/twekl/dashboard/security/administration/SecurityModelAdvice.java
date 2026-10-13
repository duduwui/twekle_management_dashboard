package com.twekl.dashboard.security.administration;

import com.twekl.dashboard.model.administration.Admin;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller advice providing the authenticated admin to all views.
 */
@ControllerAdvice
public class SecurityModelAdvice {

    private final PortalAccess access;

    public SecurityModelAdvice(PortalAccess access) {
        this.access = access;
    }

    @ModelAttribute("currentUser")
    public Admin currentUser() {
        return access.getCurrent();
    }
}
