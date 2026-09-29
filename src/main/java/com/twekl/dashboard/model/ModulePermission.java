package com.twekl.dashboard.model;

import jakarta.persistence.*;

@Entity
@Table(name = "module_permissions")
public class ModulePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String moduleKey;

    @Column(nullable = false, length = 128)
    private String moduleNameEn;

    @Column(length = 128)
    private String moduleNameAr;

    @Column(length = 128)
    private String moduleNameKu;

    @Column(length = 255)
    private String descriptionEn;

    @Column(length = 255)
    private String descriptionAr;

    @Column(length = 255)
    private String descriptionKu;

    @Column(nullable = false)
    private boolean canCreate = false;

    @Column(nullable = false)
    private boolean canRead = true;

    @Column(nullable = false)
    private boolean canUpdate = false;

    @Column(nullable = false)
    private boolean canDelete = false;

    @Column(nullable = false)
    private boolean isVisible = true;

    @Column(length = 32)
    private String badgeColor = "#35B89F";

    public ModulePermission() {}

    public ModulePermission(Long id, Long userId, String moduleKey, String moduleNameEn, String moduleNameAr, String moduleNameKu, String descriptionEn, String descriptionAr, String descriptionKu, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete, boolean isVisible, String badgeColor) {
        this.id = id;
        this.userId = userId;
        this.moduleKey = moduleKey;
        this.moduleNameEn = moduleNameEn;
        this.moduleNameAr = moduleNameAr;
        this.moduleNameKu = moduleNameKu;
        this.descriptionEn = descriptionEn;
        this.descriptionAr = descriptionAr;
        this.descriptionKu = descriptionKu;
        this.canCreate = canCreate;
        this.canRead = canRead;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
        this.isVisible = isVisible;
        this.badgeColor = (badgeColor != null) ? badgeColor : "#35B89F";
    }

    public static ModulePermissionBuilder builder() {
        return new ModulePermissionBuilder();
    }

    public static class ModulePermissionBuilder {
        private Long id;
        private Long userId;
        private String moduleKey;
        private String moduleNameEn;
        private String moduleNameAr;
        private String moduleNameKu;
        private String descriptionEn;
        private String descriptionAr;
        private String descriptionKu;
        private boolean canCreate = false;
        private boolean canRead = true;
        private boolean canUpdate = false;
        private boolean canDelete = false;
        private boolean isVisible = true;
        private String badgeColor = "#35B89F";

        public ModulePermissionBuilder id(Long id) { this.id = id; return this; }
        public ModulePermissionBuilder userId(Long userId) { this.userId = userId; return this; }
        public ModulePermissionBuilder moduleKey(String moduleKey) { this.moduleKey = moduleKey; return this; }
        public ModulePermissionBuilder moduleNameEn(String moduleNameEn) { this.moduleNameEn = moduleNameEn; return this; }
        public ModulePermissionBuilder moduleNameAr(String moduleNameAr) { this.moduleNameAr = moduleNameAr; return this; }
        public ModulePermissionBuilder moduleNameKu(String moduleNameKu) { this.moduleNameKu = moduleNameKu; return this; }
        public ModulePermissionBuilder descriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; return this; }
        public ModulePermissionBuilder descriptionAr(String descriptionAr) { this.descriptionAr = descriptionAr; return this; }
        public ModulePermissionBuilder descriptionKu(String descriptionKu) { this.descriptionKu = descriptionKu; return this; }
        public ModulePermissionBuilder canCreate(boolean canCreate) { this.canCreate = canCreate; return this; }
        public ModulePermissionBuilder canRead(boolean canRead) { this.canRead = canRead; return this; }
        public ModulePermissionBuilder canUpdate(boolean canUpdate) { this.canUpdate = canUpdate; return this; }
        public ModulePermissionBuilder canDelete(boolean canDelete) { this.canDelete = canDelete; return this; }
        public ModulePermissionBuilder isVisible(boolean isVisible) { this.isVisible = isVisible; return this; }
        public ModulePermissionBuilder badgeColor(String badgeColor) { this.badgeColor = badgeColor; return this; }

        public ModulePermission build() {
            return new ModulePermission(id, userId, moduleKey, moduleNameEn, moduleNameAr, moduleNameKu, descriptionEn, descriptionAr, descriptionKu, canCreate, canRead, canUpdate, canDelete, isVisible, badgeColor);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }

    public String getModuleNameEn() { return moduleNameEn; }
    public void setModuleNameEn(String moduleNameEn) { this.moduleNameEn = moduleNameEn; }

    public String getModuleNameAr() { return moduleNameAr; }
    public void setModuleNameAr(String moduleNameAr) { this.moduleNameAr = moduleNameAr; }

    public String getModuleNameKu() { return moduleNameKu; }
    public void setModuleNameKu(String moduleNameKu) { this.moduleNameKu = moduleNameKu; }

    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }

    public String getDescriptionAr() { return descriptionAr; }
    public void setDescriptionAr(String descriptionAr) { this.descriptionAr = descriptionAr; }

    public String getDescriptionKu() { return descriptionKu; }
    public void setDescriptionKu(String descriptionKu) { this.descriptionKu = descriptionKu; }

    public boolean isCanCreate() { return canCreate; }
    public void setCanCreate(boolean canCreate) { this.canCreate = canCreate; }

    public boolean isCanRead() { return canRead; }
    public void setCanRead(boolean canRead) { this.canRead = canRead; }

    public boolean isCanUpdate() { return canUpdate; }
    public void setCanUpdate(boolean canUpdate) { this.canUpdate = canUpdate; }

    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }

    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { isVisible = visible; }

    public String getBadgeColor() { return badgeColor; }
    public void setBadgeColor(String badgeColor) { this.badgeColor = badgeColor; }
}
