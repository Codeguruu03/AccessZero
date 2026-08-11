package com.accesszero.dto;

import com.accesszero.domain.graph.EdgeType;

public record GraphEdgeDto(
        String sourceId,
        String targetId,
        EdgeType edgeType,
        String description
) {}
