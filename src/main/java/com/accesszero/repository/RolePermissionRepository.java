package com.accesszero.repository;

import com.accesszero.domain.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, Long> {
    List<RolePermissionEntity> findByRoleId(Long roleId);
    List<RolePermissionEntity> findByRoleIdIn(List<Long> roleIds);
}
