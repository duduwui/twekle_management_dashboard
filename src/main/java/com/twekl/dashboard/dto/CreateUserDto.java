package com.twekl.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateUserDto {

    @NotBlank(message = "English name is required")
    @Size(min = 2, max = 100, message = "English name must be between 2 and 100 characters")
    private String usernameEn;

    private String usernameAr;
    private String usernameKu;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 64, message = "Password must be at least 6 characters long")
    private String password;

    @Pattern(regexp = "^(\\+?[0-9]{7,15})?$", message = "Invalid phone number format")
    private String phoneNumber;

    private String status = "ACTIVE";
    private Long roleId;

    public CreateUserDto() {}

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

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
