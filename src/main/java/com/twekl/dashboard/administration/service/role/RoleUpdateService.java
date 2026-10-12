package com.twekl.dashboard.administration.service.role;

import com.twekl.dashboard.administration.dto.role.RoleDto;
import com.twekl.dashboard.administration.exception.ResourceNotFoundException;
import com.twekl.dashboard.administration.model.role.Role;
import com.twekl.dashboard.administration.repository.role.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoleUpdateService {

    private final RoleRepository roleRepository;

    public RoleUpdateService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
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
}
