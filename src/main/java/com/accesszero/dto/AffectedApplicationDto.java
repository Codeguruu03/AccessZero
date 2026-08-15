package com.accesszero.dto;

import com.accesszero.domain.enums.ApplicationType;
import com.accesszero.domain.enums.SensitivityLevel;

public record AffectedApplicationDto(
        Long id,
        String name,
        ApplicationType type,
        SensitivityLevel sensitivityLevel,
        boolean isPrivileged,
        int accessPathCount
) {}
