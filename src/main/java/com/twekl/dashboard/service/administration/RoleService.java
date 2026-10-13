package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.RoleDto;
import com.twekl.dashboard.exception.administration.ResourceNotFoundException;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.model.administration.Role;
import com.twekl.dashboard.repository.administration.AdminRepository;
import com.twekl.dashboard.repository.administration.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service for administrative roles and permission templates.
 */
@Service
@Transactional
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AdminRepository adminRepository;

    public RoleService() {
    }

    @Autowired
    public RoleService(RoleRepository roleRepository, AdminRepository adminRepository) {
        this.roleRepository = roleRepository;
        this.adminRepository = adminRepository;
    }

    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Role> findById(Long roleId) {
        return roleRepository.findById(roleId);
    }

    @Transactional(readOnly = true)
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public long count() {
        return roleRepository.count();
    }

    public Role createRole(RoleDto req) {
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Role name is required");
        }
        if (roleRepository.existsByName(req.getName().trim())) {
            throw new IllegalArgumentException("Role name '" + req.getName() + "' already exists");
        }

        Role role = new Role(
                req.getName().trim(),
                req.isCanCreate(),
                req.isCanRead(),
                req.isCanUpdate(),
                req.isCanDelete()
        );
        return roleRepository.save(role);
    }

    public Role updateRole(Long roleId, RoleDto req) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));

        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            String newName = req.getName().trim();
            if (!newName.equalsIgnoreCase(role.getName()) && roleRepository.existsByName(newName)) {
                throw new IllegalArgumentException("Role name '" + newName + "' already exists");
            }
            role.setName(newName);
        }

        role.setCanCreate(req.isCanCreate());
        role.setCanRead(req.isCanRead());
        role.setCanUpdate(req.isCanUpdate());
        role.setCanDelete(req.isCanDelete());

        return roleRepository.save(role);
    }

    public void assignRole(Long adminId, Long roleId) {
        if (adminRepository == null) return;
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with ID: " + adminId));

        if (roleId == null || roleId == 0) {
            admin.getRoles().clear();
        } else {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));
            if (admin.getRoles() == null) {
                admin.setRoles(new HashSet<>());
            }
            admin.getRoles().clear();
            admin.getRoles().add(role);
        }
        adminRepository.save(admin);
    }

    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));

        if ("Software Engineer".equalsIgnoreCase(role.getName())) {
            throw new IllegalArgumentException("System base role 'Software Engineer' cannot be deleted");
        }

        if (adminRepository != null) {
            List<Admin> admins = adminRepository.findAll();
            for (Admin admin : admins) {
                if (admin.getRoles() != null && admin.getRoles().remove(role)) {
                    adminRepository.save(admin);
                }
            }
        }
        roleRepository.delete(role);
    }
}
