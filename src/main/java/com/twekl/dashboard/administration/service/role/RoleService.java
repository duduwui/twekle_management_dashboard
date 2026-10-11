package com.twekl.dashboard.administration.service.role;

import com.twekl.dashboard.administration.dto.role.RoleDto;
import com.twekl.dashboard.administration.exception.ResourceNotFoundException;
import com.twekl.dashboard.administration.model.role.Role;
import com.twekl.dashboard.administration.repository.role.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service managing roles and associated authorization scopes.
 * Isolated in role domain with explicit naming.
 */
@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
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
    public Optional<Role> findByRoleId(Long roleId) {
        return roleRepository.findById(roleId);
    }

    @Transactional(readOnly = true)
    public long count() {
        return roleRepository.count();
    }

    public Role createRole(Role role) {
        if (roleRepository.existsByName(role.getName())) {
            throw new IllegalArgumentException("Role '" + role.getName() + "' already exists");
        }
        return roleRepository.save(role);
    }

    public Role createRole(RoleDto dto) {
        if (roleRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Role '" + dto.getName() + "' already exists");
        }
        Role role = new Role(dto.getName().trim(), dto.isCanCreate(), dto.isCanRead(), dto.isCanUpdate(), dto.isCanDelete());
        return roleRepository.save(role);
    }

    public Role updateRole(Long roleId, Role updated) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));

        if (updated.getName() != null && !updated.getName().trim().isEmpty()) {
            String newName = updated.getName().trim();
            if (!newName.equalsIgnoreCase(role.getName()) && roleRepository.existsByName(newName)) {
                throw new IllegalArgumentException("Role '" + newName + "' already exists");
            }
            role.setName(newName);
        }

        role.setCanCreate(updated.isCanCreate());
        role.setCanRead(updated.isCanRead());
        role.setCanUpdate(updated.isCanUpdate());
        role.setCanDelete(updated.isCanDelete());

        return roleRepository.save(role);
    }

    public Role updateRole(Long roleId, RoleDto dto) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            String newName = dto.getName().trim();
            if (!newName.equalsIgnoreCase(role.getName()) && roleRepository.existsByName(newName)) {
                throw new IllegalArgumentException("Role '" + newName + "' already exists");
            }
            role.setName(newName);
        }

        role.setCanCreate(dto.isCanCreate());
        role.setCanRead(dto.isCanRead());
        role.setCanUpdate(dto.isCanUpdate());
        role.setCanDelete(dto.isCanDelete());

        return roleRepository.save(role);
    }

    public void deleteRole(Long roleId) {
        if (!roleRepository.existsById(roleId)) {
            throw new ResourceNotFoundException("Role not found with ID: " + roleId);
        }
        roleRepository.deleteRoleAssociations(roleId);
        roleRepository.deleteById(roleId);
    }
}

