package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.RoleDto;
import com.twekl.dashboard.model.administration.Role;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for managing role templates and permission sets.
 * Summarizes available role creation, assignment, and template operations.
 */
public interface RoleService {

    List<Role> findAll();

    Optional<Role> findById(Long roleId);

    Optional<Role> findByName(String name);

    long count();

    Role createRole(RoleDto req);

    Role updateRole(Long roleId, RoleDto req);

    void assignRole(Long adminId, Long roleId);

    void deleteRole(Long roleId);
}
