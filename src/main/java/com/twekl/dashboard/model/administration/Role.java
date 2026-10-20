package com.twekl.dashboard.model.administration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

/**
 * Pure JPA Entity representing Roles within the administrative portal.
 * Explicitly decoupled from Admin staff entity into its own isolated role domain.
 */
@Entity
@Table(name = "ROLES")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotBlank(message = "Role name is required")
    @Column(name = "NAME", nullable = false, unique = true, length = 50)
    private String name;

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

    public Role() {}

    public Role(String name, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete) {
        this(name, canCreate, canRead, canUpdate, canDelete, null);
    }

    public Role(String name, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete, String actions) {
        this.name = name;
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

    // Standard ID getters & explicit domain roleId bridges
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRoleId() { return id; }
    public void setRoleId(Long roleId) { this.id = roleId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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
        Role role = (Role) o;
        return Objects.equals(name, role.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

