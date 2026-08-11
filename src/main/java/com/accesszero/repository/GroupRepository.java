package com.accesszero.repository;

import com.accesszero.domain.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, Long> {
    Optional<GroupEntity> findByName(String name);
    List<GroupEntity> findByIsPrivileged(boolean isPrivileged);
}
