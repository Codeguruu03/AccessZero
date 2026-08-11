package com.accesszero.repository;

import com.accesszero.domain.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {
    List<UserSessionEntity> findByUserId(Long userId);
    List<UserSessionEntity> findByUserIdAndActive(Long userId, boolean active);
}
