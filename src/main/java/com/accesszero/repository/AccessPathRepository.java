package com.accesszero.repository;

import com.accesszero.domain.entity.AccessPathEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessPathRepository extends JpaRepository<AccessPathEntity, Long> {
    List<AccessPathEntity> findByUserId(Long userId);
    List<AccessPathEntity> findByUserIdAndRevoked(Long userId, boolean revoked);
    void deleteByUserId(Long userId);
}
