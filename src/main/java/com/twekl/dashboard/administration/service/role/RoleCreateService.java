package com.twekl.dashboard.administration.service.role;

import com.twekl.dashboard.administration.dto.role.RoleDto;
import com.twekl.dashboard.administration.model.role.Role;
import com.twekl.dashboard.administration.repository.role.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoleCreateService {

    private final RoleRepository roleRepository;

    public RoleCreateService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
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
}
