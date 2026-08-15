package com.accesszero.repository;

import com.accesszero.domain.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {
    List<AuditEventEntity> findByOperationId(Long operationId);
    List<AuditEventEntity> findByActor(String actor);
    List<AuditEventEntity> findByTarget(String target);
    List<AuditEventEntity> findByTargetOrderByTimestampDesc(String target);
    List<AuditEventEntity> findAllByOrderByTimestampDesc();
}
