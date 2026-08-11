package com.accesszero.repository;

import com.accesszero.domain.entity.ApplicationEntity;
import com.accesszero.domain.enums.SensitivityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {
    Optional<ApplicationEntity> findByName(String name);
    List<ApplicationEntity> findBySensitivityLevel(SensitivityLevel sensitivityLevel);
}
