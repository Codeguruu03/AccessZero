package com.accesszero.dto;

import com.accesszero.domain.graph.NodeType;

public record GraphNodeDto(
        String id,
        String label,
        NodeType type,
        boolean isPrivileged
) {}
