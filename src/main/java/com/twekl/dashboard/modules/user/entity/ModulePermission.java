package com.twekl.dashboard.modules.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "module_permissions")
public class ModulePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private AppUser user;

    @Column(nullable = false, length = 50)
    private String moduleKey; // e.g. software, sales, products

    @Column(nullable = false, length = 100)
    private String moduleNameEn;

    @Column(length = 100)
    private String moduleNameAr;

    @Column(length = 100)
    private String moduleNameKu;

    @Column(nullable = false)
    private boolean canCreate = false;

    @Column(nullable = false)
    private boolean canRead = true;

    @Column(nullable = false)
    private boolean canUpdate = false;

    @Column(nullable = false)
    private boolean canDelete = false;

    public ModulePermission() {}

    public ModulePermission(String moduleKey, String moduleNameEn, String moduleNameAr, String moduleNameKu,
                            boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete) {
        this.moduleKey = moduleKey;
        this.moduleNameEn = moduleNameEn;
        this.moduleNameAr = moduleNameAr;
        this.moduleNameKu = moduleNameKu;
        this.canCreate = canCreate;
        this.canRead = canRead;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }

    public String getModuleNameEn() { return moduleNameEn; }
    public void setModuleNameEn(String moduleNameEn) { this.moduleNameEn = moduleNameEn; }

    public String getModuleNameAr() { return moduleNameAr; }
    public void setModuleNameAr(String moduleNameAr) { this.moduleNameAr = moduleNameAr; }

    public String getModuleNameKu() { return moduleNameKu; }
    public void setModuleNameKu(String moduleNameKu) { this.moduleNameKu = moduleNameKu; }

    public boolean isCanCreate() { return canCreate; }
    public void setCanCreate(boolean canCreate) { this.canCreate = canCreate; }

    public boolean isCanRead() { return canRead; }
    public void setCanRead(boolean canRead) { this.canRead = canRead; }

    public boolean isCanUpdate() { return canUpdate; }
    public void setCanUpdate(boolean canUpdate) { this.canUpdate = canUpdate; }

    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }
}
