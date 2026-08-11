package com.accesszero.domain.graph;

import java.util.Objects;

public class GraphEdge {
    private final String sourceId;
    private final String targetId;
    private final EdgeType edgeType;
    private final String description;

    public GraphEdge(String sourceId, String targetId, EdgeType edgeType, String description) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.edgeType = edgeType;
        this.description = description;
    }

    public String getSourceId() { return sourceId; }
    public String getTargetId() { return targetId; }
    public EdgeType getEdgeType() { return edgeType; }
    public String getDescription() { return description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphEdge graphEdge = (GraphEdge) o;
        return Objects.equals(sourceId, graphEdge.sourceId) &&
                Objects.equals(targetId, graphEdge.targetId) &&
                edgeType == graphEdge.edgeType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId, targetId, edgeType);
    }
}
