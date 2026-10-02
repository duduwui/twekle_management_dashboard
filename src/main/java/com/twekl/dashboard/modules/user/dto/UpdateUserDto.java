package com.twekl.dashboard.modules.user.dto;

import java.util.List;

public class UpdateUserDto {

    private String usernameEn;
    private String usernameAr;
    private String usernameKu;
    private String password;
    private String phoneNumber;
    private String status;
    private List<CreateUserDto.PermissionDto> permissions;

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
    public List<CreateUserDto.PermissionDto> getPermissions() { return permissions; }
    public void setPermissions(List<CreateUserDto.PermissionDto> permissions) { this.permissions = permissions; }
}
