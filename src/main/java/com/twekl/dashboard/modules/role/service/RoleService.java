package com.twekl.dashboard.modules.role.service;

import com.twekl.dashboard.modules.role.dto.RoleDto;
import com.twekl.dashboard.modules.role.entity.Role;
import com.twekl.dashboard.modules.role.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    public Role createRole(RoleDto dto) {
        if (roleRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Role '" + dto.getName() + "' already exists");
        }
        Role role = new Role(dto.getName().trim(), dto.isCanCreate(), dto.isCanRead(), dto.isCanUpdate(), dto.isCanDelete());
        return roleRepository.save(role);
    }

    public Role updateRole(Long id, RoleDto dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));

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

    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new IllegalArgumentException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }

    public long count() {
        return roleRepository.count();
    }
}
