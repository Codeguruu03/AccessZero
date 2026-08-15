package com.accesszero.dto;

import com.accesszero.domain.enums.UserStatus;

public record IdentitySummaryDto(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String department,
        UserStatus status,
        int activeSessionsCount,
        int oauthTokensCount,
        int groupCount,
        int appCount,
        int accessPathCount,
        String riskLevel,
        int riskScore
) {}
