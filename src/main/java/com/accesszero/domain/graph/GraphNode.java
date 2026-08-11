package com.accesszero.domain.graph;

import java.util.Objects;

public class GraphNode {
    private final String id;
    private final String label;
    private final NodeType type;
    private final boolean isPrivileged;

    public GraphNode(String id, String label, NodeType type, boolean isPrivileged) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.isPrivileged = isPrivileged;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public NodeType getType() { return type; }
    public boolean isPrivileged() { return isPrivileged; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return Objects.equals(id, graphNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return label + " (" + type + ")";
    }
}
