package com.twekl.dashboard.dto.admin;

public class AdminPermissionDto {

    private String moduleKey;
    private String moduleNameEn;
    private boolean canCreate;
    private boolean canRead;
    private boolean canUpdate;
    private boolean canDelete;

    public AdminPermissionDto() {}

    public AdminPermissionDto(String moduleKey, String moduleNameEn, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete) {
        this.moduleKey = moduleKey;
        this.moduleNameEn = moduleNameEn;
        this.canCreate = canCreate;
        this.canRead = canRead;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
    }

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
}

