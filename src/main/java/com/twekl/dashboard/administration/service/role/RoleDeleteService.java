package com.twekl.dashboard.administration.service.role;

import com.twekl.dashboard.administration.exception.ResourceNotFoundException;
import com.twekl.dashboard.administration.repository.role.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoleDeleteService {

    private final RoleRepository roleRepository;

    public RoleDeleteService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void deleteRole(Long roleId) {
        if (!roleRepository.existsById(roleId)) {
            throw new ResourceNotFoundException("Role not found with ID: " + roleId);
        }
        roleRepository.deleteRoleAssociations(roleId);
        roleRepository.deleteById(roleId);
    }
}
