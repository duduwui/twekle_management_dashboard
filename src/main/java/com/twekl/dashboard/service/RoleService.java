package com.twekl.dashboard.service;

import com.twekl.dashboard.model.Role;
import com.twekl.dashboard.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Optional<Role> getRoleById(Long id) {
        return roleRepository.findById(id);
    }

    public Optional<Role> getRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    @Transactional
    public Role createRole(Role role) {
        if (roleRepository.existsByNameIgnoreCase(role.getName().trim())) {
            throw new IllegalArgumentException("Role '" + role.getName() + "' already exists (case-insensitive)");
        }
        return roleRepository.save(role);
    }

    @Transactional
    public Role updateRole(Long id, Role updated) {
        Role existing = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));
        if (updated.getName() != null && !updated.getName().trim().isEmpty()) {
            if (roleRepository.existsByNameIgnoreCaseAndIdNot(updated.getName().trim(), id)) {
                throw new IllegalArgumentException("Role '" + updated.getName() + "' already exists (case-insensitive)");
            }
            existing.setName(updated.getName().trim());
        }
        existing.setCanCreate(updated.isCanCreate());
        existing.setCanRead(updated.isCanRead());
        existing.setCanUpdate(updated.isCanUpdate());
        existing.setCanDelete(updated.isCanDelete());
        return roleRepository.save(existing);
    }

    @Transactional
    public Role toggleRolePermission(Long id, String field, boolean value) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));
        switch (field.toLowerCase()) {
            case "create":
            case "cancreate":
                role.setCanCreate(value);
                break;
            case "read":
            case "canread":
                role.setCanRead(value);
                break;
            case "update":
            case "canupdate":
                role.setCanUpdate(value);
                break;
            case "delete":
            case "candelete":
                role.setCanDelete(value);
                break;
            case "full_crud":
                role.setCanCreate(value);
                role.setCanRead(value);
                role.setCanUpdate(value);
                role.setCanDelete(value);
                break;
            default:
                throw new IllegalArgumentException("Invalid permission field: " + field);
        }
        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new IllegalArgumentException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }
}
