package com.twekl.dashboard.controller;

import com.twekl.dashboard.model.Role;
import com.twekl.dashboard.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")

public class RoleApiController {

    private final RoleService roleService;

    @Autowired
    public RoleApiController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody Role role) {
        if (role.getName() == null || role.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role name is required"));
        }
        Role created = roleService.createRole(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleRolePermission(@PathVariable Long id,
                                                  @RequestParam("field") String field,
                                                  @RequestParam("value") boolean value) {
        Role updated = roleService.toggleRolePermission(id, field, value);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(Map.of("message", "Role deleted successfully", "id", id));
    }
}
