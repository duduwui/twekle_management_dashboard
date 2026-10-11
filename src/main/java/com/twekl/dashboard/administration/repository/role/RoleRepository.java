package com.twekl.dashboard.administration.repository.role;

import com.twekl.dashboard.administration.model.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Role entities.
 * Isolated in role domain.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ADMIN_ROLES WHERE ROLE_ID = :roleId", nativeQuery = true)
    void deleteRoleAssociations(@Param("roleId") Long roleId);
}

