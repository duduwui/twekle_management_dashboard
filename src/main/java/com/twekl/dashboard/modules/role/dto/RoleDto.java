package com.twekl.dashboard.modules.role.dto;

import jakarta.validation.constraints.NotBlank;

public class RoleDto {

    @NotBlank(message = "Role name is required")
    private String name;

    private boolean canCreate = false;
    private boolean canRead = true;
    private boolean canUpdate = false;
    private boolean canDelete = false;

    public RoleDto() {}

    public RoleDto(String name, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete) {
        this.name = name;
        this.canCreate = canCreate;
        this.canRead = canRead;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isCanCreate() { return canCreate; }
    public void setCanCreate(boolean canCreate) { this.canCreate = canCreate; }

    public boolean isCanRead() { return canRead; }
    public void setCanRead(boolean canRead) { this.canRead = canRead; }

    public boolean isCanUpdate() { return canUpdate; }
    public void setCanUpdate(boolean canUpdate) { this.canUpdate = canUpdate; }

    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }
}
