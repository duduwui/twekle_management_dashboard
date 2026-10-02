package com.twekl.dashboard.modules.user.repository;

import com.twekl.dashboard.modules.user.entity.ModulePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModulePermissionRepository extends JpaRepository<ModulePermission, Long> {
    List<ModulePermission> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
