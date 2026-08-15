package com.accesszero.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record VerificationResultDto(
        Long operationId,
        Long userId,
        String username,
        String overallStatus, // CONTAINED, PARTIAL, FAILED
        int accessPathsFound,
        int accessPathsRevoked,
        int requiresManualActionCount,
        Map<String, ProviderVerificationDto> providerResults,
        List<String> remainingRisks,
        List<String> manualActionSteps,
        LocalDateTime verifiedAt
) {}
