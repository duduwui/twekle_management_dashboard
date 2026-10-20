package com.twekl.dashboard.model.administration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * JPA Entity storing module-level CRUD permissions for an administrator.
 */
@Entity
@Table(name = "ADMIN_PERMISSIONS")
public class AdminPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADMIN_ID", nullable = false)
    @JsonIgnore
    private Admin admin;

    @Column(name = "MODULE_KEY", nullable = false, length = 64)
    private String moduleKey;

    @Column(name = "MODULE_NAME_EN", length = 128)
    private String moduleNameEn;

    @Column(name = "CAN_CREATE", nullable = false)
    private boolean canCreate = false;

    @Column(name = "CAN_READ", nullable = false)
    private boolean canRead = true;

    @Column(name = "CAN_UPDATE", nullable = false)
    private boolean canUpdate = false;

    @Column(name = "CAN_DELETE", nullable = false)
    private boolean canDelete = false;

    @Column(name = "ACTIONS", length = 1000)
    private String actions;

    public AdminPermission() {}

    public AdminPermission(Admin admin, String moduleKey, String moduleNameEn, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete) {
        this(admin, moduleKey, moduleNameEn, canCreate, canRead, canUpdate, canDelete, null);
    }

    public AdminPermission(Admin admin, String moduleKey, String moduleNameEn, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete, String actions) {
        this.admin = admin;
        this.moduleKey = moduleKey;
        this.moduleNameEn = moduleNameEn;
        this.canCreate = canCreate;
        this.canRead = canRead;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
        this.actions = actions;
    }

    public String getActions() { return actions; }
    public void setActions(String actions) { this.actions = actions; }

    public java.util.Set<String> getActionSet() {
        if (actions == null || actions.trim().isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return java.util.Arrays.stream(actions.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toSet());
    }

    public void setActionSet(java.util.Collection<String> actionKeys) {
        if (actionKeys == null || actionKeys.isEmpty()) {
            this.actions = null;
        } else {
            this.actions = String.join(",", actionKeys);
        }
    }

    public boolean hasAction(String actionKey) {
        if (actionKey == null || actionKey.trim().isEmpty()) return false;
        String target = actionKey.trim().toUpperCase();
        java.util.Set<String> set = getActionSet();
        if (set.contains(target)) {
            return true;
        }
        if (set.isEmpty()) {
            return ActionPermission.fromKey(target)
                    .map(ap -> ap.matchesFallback(canCreate, canRead, canUpdate, canDelete))
                    .orElse(canRead);
        }
        return false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }

    public String getModuleNameEn() { return moduleNameEn; }
    public void setModuleNameEn(String moduleNameEn) { this.moduleNameEn = moduleNameEn; }

    public boolean isCanCreate() { return canCreate; }
    public boolean canCreate() { return canCreate; }
    public void setCanCreate(boolean canCreate) { this.canCreate = canCreate; }

    public boolean isCanRead() { return canRead; }
    public boolean canRead() { return canRead; }
    public void setCanRead(boolean canRead) { this.canRead = canRead; }

    public boolean isCanUpdate() { return canUpdate; }
    public boolean canUpdate() { return canUpdate; }
    public void setCanUpdate(boolean canUpdate) { this.canUpdate = canUpdate; }

    public boolean isCanDelete() { return canDelete; }
    public boolean canDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminPermission that = (AdminPermission) o;
        return Objects.equals(moduleKey, that.moduleKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleKey);
    }
}

