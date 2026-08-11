package com.accesszero.dto;

import com.accesszero.domain.enums.UserStatus;
import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String department,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
