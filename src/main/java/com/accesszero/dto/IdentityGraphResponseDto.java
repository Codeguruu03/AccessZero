package com.accesszero.dto;

import java.util.List;

public record IdentityGraphResponseDto(
        Long userId,
        int totalNodes,
        List<GraphNodeDto> nodes,
        List<GraphEdgeDto> edges
) {}
