package com.twekl.dashboard.administration.service.role;

import com.twekl.dashboard.administration.model.role.Role;
import com.twekl.dashboard.administration.repository.role.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RoleQueryService {

    private final RoleRepository roleRepository;

    public RoleQueryService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public Optional<Role> findById(Long roleId) {
        return roleRepository.findById(roleId);
    }

    public Optional<Role> findByRoleId(Long roleId) {
        return roleRepository.findById(roleId);
    }

    public long count() {
        return roleRepository.count();
    }
}
