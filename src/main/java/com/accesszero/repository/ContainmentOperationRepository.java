package com.accesszero.repository;

import com.accesszero.domain.entity.ContainmentOperationEntity;
import com.accesszero.domain.enums.ContainmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContainmentOperationRepository extends JpaRepository<ContainmentOperationEntity, Long> {
    List<ContainmentOperationEntity> findByTargetUserId(Long targetUserId);
    Optional<ContainmentOperationEntity> findFirstByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
    List<ContainmentOperationEntity> findByStatus(ContainmentStatus status);
}
