package com.accesszero.graph;

import com.accesszero.domain.graph.GraphEdge;
import com.accesszero.domain.graph.GraphNode;
import com.accesszero.domain.graph.IdentityGraph;
import com.accesszero.domain.graph.NodeType;
import com.accesszero.domain.graph.EdgeType;
import com.accesszero.service.GraphMermaidExporter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphMermaidExporterTest {

    @Test
    void testExportToMermaid() {
        GraphMermaidExporter exporter = new GraphMermaidExporter();
        IdentityGraph graph = new IdentityGraph();

        GraphNode userNode = new GraphNode("USER:1", "rahul.sharma", NodeType.USER, false);
        GraphNode groupNode = new GraphNode("GROUP:10", "payroll-admin", NodeType.GROUP, true);

        graph.addNode(userNode);
        graph.addNode(groupNode);
        graph.addEdge(new GraphEdge("USER:1", "GROUP:10", EdgeType.MEMBER_OF, "Member of group"));

        String mermaid = exporter.exportToMermaid(graph, "rahul.sharma");

        assertNotNull(mermaid);
        assertTrue(mermaid.contains("graph TD"));
        assertTrue(mermaid.contains("rahul.sharma"));
        assertTrue(mermaid.contains("payroll-admin"));
        assertTrue(mermaid.contains(":::privileged"));
    }
}
