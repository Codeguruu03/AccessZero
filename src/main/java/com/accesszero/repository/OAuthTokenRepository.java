package com.accesszero.repository;

import com.accesszero.domain.entity.OAuthTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthTokenEntity, Long> {
    List<OAuthTokenEntity> findByUserId(Long userId);
    List<OAuthTokenEntity> findByUserIdAndRevoked(Long userId, boolean revoked);
}
