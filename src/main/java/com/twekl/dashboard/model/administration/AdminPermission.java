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

    public AdminPermission() {}

    public AdminPermission(Admin admin, String moduleKey, String moduleNameEn, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete) {
        this.admin = admin;
        this.moduleKey = moduleKey;
        this.moduleNameEn = moduleNameEn;
        this.canCreate = canCreate;
        this.canRead = canRead;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
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

