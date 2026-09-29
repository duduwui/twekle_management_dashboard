package com.twekl.dashboard.model;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(nullable = false)
    private boolean canCreate = false;

    @Column(nullable = false)
    private boolean canRead = true;

    @Column(nullable = false)
    private boolean canUpdate = false;

    @Column(nullable = false)
    private boolean canDelete = false;

    public Role() {}

    public Role(Long id, String name, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete) {
        this.id = id;
        this.name = name;
        this.canCreate = canCreate;
        this.canRead = canRead;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
    }

    public static RoleBuilder builder() {
        return new RoleBuilder();
    }

    public static class RoleBuilder {
        private Long id;
        private String name;
        private boolean canCreate = false;
        private boolean canRead = true;
        private boolean canUpdate = false;
        private boolean canDelete = false;

        public RoleBuilder id(Long id) { this.id = id; return this; }
        public RoleBuilder name(String name) { this.name = name; return this; }
        public RoleBuilder canCreate(boolean canCreate) { this.canCreate = canCreate; return this; }
        public RoleBuilder canRead(boolean canRead) { this.canRead = canRead; return this; }
        public RoleBuilder canUpdate(boolean canUpdate) { this.canUpdate = canUpdate; return this; }
        public RoleBuilder canDelete(boolean canDelete) { this.canDelete = canDelete; return this; }

        public Role build() {
            return new Role(id, name, canCreate, canRead, canUpdate, canDelete);
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
