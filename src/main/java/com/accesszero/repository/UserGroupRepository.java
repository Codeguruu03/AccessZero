package com.accesszero.repository;

import com.accesszero.domain.entity.UserGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroupEntity, Long> {
    List<UserGroupEntity> findByUserId(Long userId);
    List<UserGroupEntity> findByGroupId(Long groupId);
    void deleteByUserIdAndGroupId(Long userId, Long groupId);
}
