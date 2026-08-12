package com.accesszero.controller;

import com.accesszero.domain.entity.AccessPathEntity;
import com.accesszero.domain.entity.UserEntity;
import com.accesszero.domain.graph.GraphEdge;
import com.accesszero.domain.graph.GraphNode;
import com.accesszero.domain.graph.IdentityGraph;
import com.accesszero.dto.AccessPathSummaryDto;
import com.accesszero.dto.GraphEdgeDto;
import com.accesszero.dto.GraphNodeDto;
import com.accesszero.dto.IdentityGraphResponseDto;
import com.accesszero.repository.UserRepository;
import com.accesszero.service.AccessPathResolverService;
import com.accesszero.service.GraphMermaidExporter;
import com.accesszero.service.IdentityGraphEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/identities")
public class IdentityGraphController {

    private final IdentityGraphEngine identityGraphEngine;
    private final AccessPathResolverService accessPathResolverService;
    private final GraphMermaidExporter graphMermaidExporter;
    private final UserRepository userRepository;

    public IdentityGraphController(
            IdentityGraphEngine identityGraphEngine,
            AccessPathResolverService accessPathResolverService,
            GraphMermaidExporter graphMermaidExporter,
            UserRepository userRepository
    ) {
        this.identityGraphEngine = identityGraphEngine;
        this.accessPathResolverService = accessPathResolverService;
        this.graphMermaidExporter = graphMermaidExporter;
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}/graph")
    public ResponseEntity<IdentityGraphResponseDto> getIdentityGraph(@PathVariable Long userId) {
        IdentityGraph graph = identityGraphEngine.buildGraphForUser(userId);

        List<GraphNodeDto> nodeDtos = new ArrayList<>();
        List<GraphEdgeDto> edgeDtos = new ArrayList<>();

        for (GraphNode node : graph.getAllNodes()) {
            nodeDtos.add(new GraphNodeDto(node.getId(), node.getLabel(), node.getType(), node.isPrivileged()));
            for (GraphEdge edge : graph.getOutgoingEdges(node.getId())) {
                edgeDtos.add(new GraphEdgeDto(edge.getSourceId(), edge.getTargetId(), edge.getEdgeType(), edge.getDescription()));
            }
        }

        IdentityGraphResponseDto response = new IdentityGraphResponseDto(
                userId,
                nodeDtos.size(),
                nodeDtos,
                edgeDtos
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{userId}/graph/mermaid", produces = "text/plain")
    public ResponseEntity<String> getIdentityGraphMermaid(@PathVariable Long userId) {
        IdentityGraph graph = identityGraphEngine.buildGraphForUser(userId);
        String username = userRepository.findById(userId)
                .map(UserEntity::getUsername)
                .orElse("user_" + userId);

        String mermaidDiagram = graphMermaidExporter.exportToMermaid(graph, username);
        return ResponseEntity.ok(mermaidDiagram);
    }

    @GetMapping("/{userId}/access-paths")
    public ResponseEntity<List<AccessPathEntity>> getAccessPaths(@PathVariable Long userId) {
        List<AccessPathEntity> paths = accessPathResolverService.resolveAndPersistAccessPaths(userId);
        return ResponseEntity.ok(paths);
    }

    @GetMapping("/{userId}/access-paths/summary")
    public ResponseEntity<AccessPathSummaryDto> getAccessPathSummary(@PathVariable Long userId) {
        List<AccessPathEntity> paths = accessPathResolverService.resolveAndPersistAccessPaths(userId);
        String username = userRepository.findById(userId)
                .map(UserEntity::getUsername)
                .orElse("user_" + userId);

        int totalPaths = paths.size();
        int privilegedCount = (int) paths.stream().filter(AccessPathEntity::isPrivileged).count();
        Map<String, Long> byType = paths.stream()
                .collect(Collectors.groupingBy(p -> p.getPathType().name(), Collectors.counting()));

        AccessPathSummaryDto summary = new AccessPathSummaryDto(
                userId,
                username,
                totalPaths,
                privilegedCount,
                byType
        );

        return ResponseEntity.ok(summary);
    }

    @PostMapping("/{userId}/access-paths/recalculate")
    public ResponseEntity<List<AccessPathEntity>> recalculateAccessPaths(@PathVariable Long userId) {
        List<AccessPathEntity> paths = accessPathResolverService.resolveAndPersistAccessPaths(userId);
        return ResponseEntity.ok(paths);
    }
}
