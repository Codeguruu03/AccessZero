package com.accesszero.dto;

import com.accesszero.domain.enums.GroupType;

public record GroupDto(
        Long id,
        String name,
        String description,
        boolean isPrivileged,
        GroupType type
) {}
