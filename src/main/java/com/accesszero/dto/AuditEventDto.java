package com.accesszero.dto;

import java.time.LocalDateTime;

public record AuditEventDto(
        Long id,
        Long operationId,
        String actor,
        String action,
        String target,
        String result,
        String detailsJson,
        LocalDateTime timestamp,
        String checksum
) {}
