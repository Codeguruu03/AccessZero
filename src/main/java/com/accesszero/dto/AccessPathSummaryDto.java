package com.accesszero.dto;

import java.util.Map;

public record AccessPathSummaryDto(
        Long userId,
        String username,
        int totalAccessPaths,
        int privilegedAccessPaths,
        Map<String, Long> pathsByTypeCount
) {}
