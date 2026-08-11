package com.accesszero.repository;

import com.accesszero.domain.entity.GroupRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRoleRepository extends JpaRepository<GroupRoleEntity, Long> {
    List<GroupRoleEntity> findByGroupId(Long groupId);
    List<GroupRoleEntity> findByGroupIdIn(List<Long> groupIds);
}
