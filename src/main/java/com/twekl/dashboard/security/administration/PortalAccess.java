package com.twekl.dashboard.security.administration;

import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.service.administration.AdminService;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/** Loads live permissions once per HTTP request, so revocations apply on the next request. */
@Component("portalAccess")
@RequestScope
public class PortalAccess {
    private final AdminService admins;
    private Admin current;
    private boolean loaded;

    public PortalAccess(AdminService admins) {
        this.admins = admins;
    }

    public Admin getCurrent() {
        if (!loaded) {
            current = admins.getCurrentAdmin().filter(Admin::isActive).orElse(null);
            loaded = true;
        }
        return current;
    }

    public boolean allows(String module, String action) {
        return admins.hasPermission(getCurrent(), module, action);
    }

    public boolean staff() {
        Admin a = getCurrent();
        return a != null && allows("ADMIN_MANAGEMENT", "READ");
    }

    public boolean roles() {
        Admin a = getCurrent();
        return a != null && allows("ROLE_MANAGEMENT", "READ");
    }
}
