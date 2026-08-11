package com.accesszero.domain.graph;

import java.util.*;

public class IdentityGraph {

    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final Map<String, List<GraphEdge>> adjacencyList = new HashMap<>();

    public void addNode(GraphNode node) {
        nodes.put(node.getId(), node);
        adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
    }

    public void addEdge(GraphEdge edge) {
        if (!nodes.containsKey(edge.getSourceId()) || !nodes.containsKey(edge.getTargetId())) {
            throw new IllegalArgumentException("Source and Target nodes must exist in graph before adding edge.");
        }
        adjacencyList.get(edge.getSourceId()).add(edge);
    }

    public GraphNode getNode(String id) {
        return nodes.get(id);
    }

    public Collection<GraphNode> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public List<GraphEdge> getOutgoingEdges(String nodeId) {
        return Collections.unmodifiableList(adjacencyList.getOrDefault(nodeId, Collections.emptyList()));
    }

    /**
     * Traverses graph using Depth First Search (DFS) to find all directed paths from startNodeId to any node of targetType.
     */
    public List<List<GraphNode>> findAllPathsToType(String startNodeId, NodeType targetType) {
        List<List<GraphNode>> allPaths = new ArrayList<>();
        if (!nodes.containsKey(startNodeId)) {
            return allPaths;
        }

        Set<String> visited = new HashSet<>();
        List<GraphNode> currentPath = new ArrayList<>();
        dfs(startNodeId, targetType, visited, currentPath, allPaths);
        return allPaths;
    }

    private void dfs(String currentId, NodeType targetType, Set<String> visited, List<GraphNode> currentPath, List<List<GraphNode>> resultPaths) {
        visited.add(currentId);
        GraphNode currentNode = nodes.get(currentId);
        currentPath.add(currentNode);

        if (currentNode.getType() == targetType && currentPath.size() > 1) {
            resultPaths.add(new ArrayList<>(currentPath));
        }

        for (GraphEdge edge : getOutgoingEdges(currentId)) {
            String nextId = edge.getTargetId();
            if (!visited.contains(nextId)) {
                dfs(nextId, targetType, visited, currentPath, resultPaths);
            }
        }

        currentPath.remove(currentPath.size() - 1);
        visited.remove(currentId);
    }
}
