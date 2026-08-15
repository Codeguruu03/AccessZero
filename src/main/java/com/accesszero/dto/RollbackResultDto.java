package com.accesszero.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RollbackResultDto(
        Long operationId,
        Long userId,
        String username,
        String status,
        String rolledBackBy,
        List<String> actionsExecuted,
        LocalDateTime rolledBackAt
) {}
