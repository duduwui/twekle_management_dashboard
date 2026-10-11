package com.twekl.dashboard.administration.dto.role;

/**
 * Data Transfer Object for Role templates.
 * Isolated in role domain with explicit naming.
 */
public class RoleDto {

    private Long id;
    private String name;
    private boolean canCreate;
    private boolean canRead;
    private boolean canUpdate;
    private boolean canDelete;

    public RoleDto() {}

    public RoleDto(String name, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete) {
        this.name = name;
        this.canCreate = canCreate;
        this.canRead = canRead;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
    }

    public RoleDto(Long id, String name, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete) {
        this.id = id;
        this.name = name;
        this.canCreate = canCreate;
        this.canRead = canRead;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRoleId() { return id; }
    public void setRoleId(Long roleId) { this.id = roleId; }

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

