package com.accesszero.dto;

public record ContainmentRequestDto(
        Long userId,
        String username,
        String requestedBy,
        String reason,
        boolean emergencyOverride
) {}
