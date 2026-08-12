package com.accesszero.service;

import com.accesszero.domain.graph.GraphEdge;
import com.accesszero.domain.graph.GraphNode;
import com.accesszero.domain.graph.IdentityGraph;
import org.springframework.stereotype.Service;

@Service
public class GraphMermaidExporter {

    public String exportToMermaid(IdentityGraph graph, String username) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph TD\n");
        sb.append("  %% Identity Access Graph for ").append(username).append("\n");

        for (GraphNode node : graph.getAllNodes()) {
            String sanitizedId = sanitizeId(node.getId());
            String style = node.isPrivileged() ? ":::privileged" : "";
            sb.append("  ").append(sanitizedId).append("[\"").append(node.getLabel()).append("\"]").append(style).append("\n");

            for (GraphEdge edge : graph.getOutgoingEdges(node.getId())) {
                String targetSanitized = sanitizeId(edge.getTargetId());
                sb.append("  ").append(sanitizedId).append(" -->|\"").append(edge.getEdgeType()).append("\"| ").append(targetSanitized).append("\n");
            }
        }

        sb.append("\n  classDef privileged fill:#ff4d4f,stroke:#333,stroke-width:2px,color:#fff;\n");
        return sb.toString();
    }

    private String sanitizeId(String id) {
        return id.replace(":", "_").replace("-", "_").replace(".", "_");
    }
}
