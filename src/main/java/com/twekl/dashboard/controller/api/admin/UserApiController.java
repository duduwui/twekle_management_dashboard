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

@RestController
@RequestMapping("/api/users")

public class UserApiController {

    private final AppUserService userService;

    @Autowired
    public UserApiController(AppUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<List<AppUser>> getAllUsers(@RequestParam(value = "query", required = false) String query) {
        if (query != null && !query.trim().isEmpty()) {
            return ResponseEntity.ok(userService.searchUsers(query));
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<AppUser> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/modules")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<List<ModulePermission>> getUserModules(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getPermissionsForUser(id));
    }

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

    @PatchMapping("/{userId}/modules/{moduleKey}/toggle")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<ModulePermission> toggleModulePermission(@PathVariable Long userId,
                                                                   @PathVariable String moduleKey,
                                                                   @RequestParam("field") String field,
                                                                   @RequestParam("value") boolean value) {
        ModulePermission updated = userService.updateModulePermission(userId, moduleKey, field, value);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{userId}/modules")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<ModulePermission> addCustomModule(@PathVariable Long userId, @Valid @RequestBody ModulePermission module) {
        ModulePermission added = userService.addCustomModuleForUser(userId, module);
        return ResponseEntity.status(HttpStatus.CREATED).body(added);
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_MANAGE_USERS')")
    public ResponseEntity<AppUser> toggleStatus(@PathVariable Long id) {
        AppUser toggled = userService.toggleUserStatus(id);
        return ResponseEntity.ok(toggled);
    }

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
