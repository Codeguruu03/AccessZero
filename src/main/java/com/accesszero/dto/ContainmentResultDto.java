package com.accesszero.dto;

import com.accesszero.domain.enums.ContainmentStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ContainmentResultDto(
        Long operationId,
        Long userId,
        String username,
        ContainmentStatus status,
        String requestedBy,
        String approvedBy,
        String reason,
        boolean requiresApproval,
        int accessPathsFound,
        int accessPathsRevoked,
        int requiresManualAction,
        List<String> actionsExecuted,
        VerificationResultDto verificationResult,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
