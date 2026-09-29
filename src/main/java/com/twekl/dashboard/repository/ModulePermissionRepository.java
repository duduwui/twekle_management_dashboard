package com.twekl.dashboard.repository;

import com.twekl.dashboard.model.ModulePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModulePermissionRepository extends JpaRepository<ModulePermission, Long> {
    List<ModulePermission> findByUserId(Long userId);
    Optional<ModulePermission> findByUserIdAndModuleKey(Long userId, String moduleKey);
    void deleteByUserId(Long userId);
}
