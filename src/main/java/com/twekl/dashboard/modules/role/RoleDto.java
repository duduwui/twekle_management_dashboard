package com.twekl.dashboard.modules.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RoleDto {

    private Long id;

    @NotBlank(message = "Role name is required")
    @Size(max = 64, message = "Role name must not exceed 64 characters")
    private String name;

    private boolean canCreate = false;
    private boolean canRead = true;
    private boolean canUpdate = false;
    private boolean canDelete = false;

    public RoleDto() {}

    public RoleDto(Long id, String name, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete) {
        this.id = id;
        this.name = name;
        this.canCreate = canCreate;
        this.canRead = canRead;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private boolean canCreate = false;
        private boolean canRead = true;
        private boolean canUpdate = false;
        private boolean canDelete = false;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder canCreate(boolean canCreate) { this.canCreate = canCreate; return this; }
        public Builder canRead(boolean canRead) { this.canRead = canRead; return this; }
        public Builder canUpdate(boolean canUpdate) { this.canUpdate = canUpdate; return this; }
        public Builder canDelete(boolean canDelete) { this.canDelete = canDelete; return this; }
        public RoleDto build() {
            return new RoleDto(id, name, canCreate, canRead, canUpdate, canDelete);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
