package com.twekl.dashboard.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateUserDto {

    @NotBlank(message = "English username is required")
    private String usernameEn;

    private String usernameAr;
    private String usernameKu;

    @NotBlank(message = "Password is required")
    private String password;

    private String phoneNumber;
    private String status = "ACTIVE";
    private List<PermissionDto> permissions;

    public static class PermissionDto {
        private String moduleKey;
        private String moduleNameEn;
        private String moduleNameAr;
        private String moduleNameKu;
        private boolean canCreate;
        private boolean canRead;
        private boolean canUpdate;
        private boolean canDelete;

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

    // Getters and Setters
    public String getUsernameEn() { return usernameEn; }
    public void setUsernameEn(String usernameEn) { this.usernameEn = usernameEn; }
    public String getUsernameAr() { return usernameAr; }
    public void setUsernameAr(String usernameAr) { this.usernameAr = usernameAr; }
    public String getUsernameKu() { return usernameKu; }
    public void setUsernameKu(String usernameKu) { this.usernameKu = usernameKu; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<PermissionDto> getPermissions() { return permissions; }
    public void setPermissions(List<PermissionDto> permissions) { this.permissions = permissions; }
}
