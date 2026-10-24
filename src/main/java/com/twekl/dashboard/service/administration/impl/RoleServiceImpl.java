package com.twekl.dashboard.service.administration.impl;

import com.twekl.dashboard.dto.administration.RoleDto;
import com.twekl.dashboard.exception.ResourceNotFoundException;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.model.administration.Role;
import com.twekl.dashboard.repository.administration.AdminRepository;
import com.twekl.dashboard.repository.administration.RoleRepository;
import com.twekl.dashboard.service.administration.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Production implementation of RoleService.
 * Manages administrative roles and security templates.
 */
@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final AdminRepository adminRepository;

    @Autowired
    public RoleServiceImpl(final RoleRepository roleRepository, final AdminRepository adminRepository) {
        this.roleRepository = roleRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findById(Long roleId) {
        return roleRepository.findById(roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return roleRepository.count();
    }

    @Override
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
                req.isCanDelete(),
                req.getActions()
        );
        return roleRepository.save(role);
    }

    @Override
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
        if (req.getActions() != null) {
            role.setActions(req.getActions());
        }

        return roleRepository.save(role);
    }

    @Override
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
