package com.accesszero.repository;

import com.accesszero.domain.entity.SAMLAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SAMLAssignmentRepository extends JpaRepository<SAMLAssignmentEntity, Long> {
    List<SAMLAssignmentEntity> findByUserId(Long userId);
    List<SAMLAssignmentEntity> findByApplicationId(Long applicationId);
}
