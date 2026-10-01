package com.twekl.dashboard.controller.api.admin;

import com.twekl.dashboard.model.Role;
import com.twekl.dashboard.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Roles", description = "Role templates and global permission presets")
@RestController
@RequestMapping("/api/roles")
public class RoleApiController {

    private final RoleService roleService;

    @Autowired
    public RoleApiController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "List all roles", description = "Retrieves all pre-configured role permission templates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('PERMISSION_CREATE_ROLES')")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @Operation(summary = "Create role template", description = "Defines a new role template with CRUD permission defaults")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_CREATE_ROLES')")
    public ResponseEntity<?> createRole(@RequestBody Role role) {
        if (role.getName() == null || role.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role name is required"));
        }
        Role created = roleService.createRole(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Toggle role permission", description = "Modifies individual permission flag for this role template")
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_CREATE_ROLES')")
    public ResponseEntity<?> toggleRolePermission(@PathVariable Long id,
                                                  @RequestParam("field") String field,
                                                  @RequestParam("value") boolean value) {
        Role updated = roleService.toggleRolePermission(id, field, value);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete role template", description = "Deletes a role template by its ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_CREATE_ROLES')")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(Map.of("message", "Role deleted successfully", "id", id));
    }
}
