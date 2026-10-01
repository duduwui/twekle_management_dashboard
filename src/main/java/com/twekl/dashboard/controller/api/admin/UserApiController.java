package com.twekl.dashboard.controller.api.admin;

import com.twekl.dashboard.dto.CreateUserDto;
import com.twekl.dashboard.dto.UpdateUserDto;
import com.twekl.dashboard.model.AppUser;
import com.twekl.dashboard.model.ModulePermission;
import com.twekl.dashboard.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Users", description = "Enterprise Staff accounts, localized usernames, and granular CRUD permissions")
@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final AppUserService userService;

    @Autowired
    public UserApiController(AppUserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List all staff users", description = "Retrieves all user accounts with multilingual username support and keyword search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<List<AppUser>> getAllUsers(@RequestParam(value = "query", required = false) String query) {
        if (query != null && !query.trim().isEmpty()) {
            return ResponseEntity.ok(userService.searchUsers(query));
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Get user by ID", description = "Retrieves staff user details")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<AppUser> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get user module permissions", description = "Retrieves granular module CRUD matrix assigned to this user")
    @GetMapping("/{id}/modules")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<List<ModulePermission>> getUserModules(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getPermissionsForUser(id));
    }

    @Operation(summary = "Create staff user", description = "Provisions a new staff user with multilingual metadata and default module permissions")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<AppUser> createUser(@Valid @RequestBody CreateUserDto dto) {
        AppUser user = AppUser.builder()
                .usernameEn(dto.getUsernameEn().trim())
                .usernameAr(dto.getUsernameAr())
                .usernameKu(dto.getUsernameKu())
                .password(dto.getPassword().trim())
                .phoneNumber(dto.getPhoneNumber())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .build();

        AppUser created = userService.createUser(user, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update staff user", description = "Modifies user username translations, contact details, and status")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<AppUser> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDto dto) {
        AppUser user = new AppUser();
        user.setUsernameEn(dto.getUsernameEn().trim());
        user.setUsernameAr(dto.getUsernameAr());
        user.setUsernameKu(dto.getUsernameKu());
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(dto.getPassword().trim());
        }
        user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }

        AppUser updated = userService.updateUser(id, user);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Toggle user module permission", description = "Toggles individual CRUD flag (canCreate, canRead, canUpdate, canDelete) for a module")
    @PatchMapping("/{userId}/modules/{moduleKey}/toggle")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<ModulePermission> toggleModulePermission(@PathVariable Long userId,
                                                                   @PathVariable String moduleKey,
                                                                   @RequestParam("field") String field,
                                                                   @RequestParam("value") boolean value) {
        ModulePermission updated = userService.updateModulePermission(userId, moduleKey, field, value);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Add custom module for user", description = "Adds a custom module permission row for a specific user")
    @PostMapping("/{userId}/modules")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<ModulePermission> addCustomModule(@PathVariable Long userId, @Valid @RequestBody ModulePermission module) {
        ModulePermission added = userService.addCustomModuleForUser(userId, module);
        return ResponseEntity.status(HttpStatus.CREATED).body(added);
    }

    @Operation(summary = "Toggle user active status", description = "Switches staff user state between ACTIVE and INACTIVE")
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<AppUser> toggleStatus(@PathVariable Long id) {
        AppUser toggled = userService.toggleUserStatus(id);
        return ResponseEntity.ok(toggled);
    }

    @Operation(summary = "Delete staff user", description = "Permanently removes user account and associated permission records")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User deleted successfully",
                "id", id
        ));
    }
}
