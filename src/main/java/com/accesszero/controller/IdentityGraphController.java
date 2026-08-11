package com.accesszero.controller;

import com.accesszero.domain.entity.AccessPathEntity;
import com.accesszero.domain.graph.GraphEdge;
import com.accesszero.domain.graph.GraphNode;
import com.accesszero.domain.graph.IdentityGraph;
import com.accesszero.dto.GraphEdgeDto;
import com.accesszero.dto.GraphNodeDto;
import com.accesszero.dto.IdentityGraphResponseDto;
import com.accesszero.service.AccessPathResolverService;
import com.accesszero.service.IdentityGraphEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/identities")
public class IdentityGraphController {

    private final IdentityGraphEngine identityGraphEngine;
    private final AccessPathResolverService accessPathResolverService;

    public IdentityGraphController(IdentityGraphEngine identityGraphEngine, AccessPathResolverService accessPathResolverService) {
        this.identityGraphEngine = identityGraphEngine;
        this.accessPathResolverService = accessPathResolverService;
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

    @GetMapping("/{userId}/access-paths")
    public ResponseEntity<List<AccessPathEntity>> getAccessPaths(@PathVariable Long userId) {
        List<AccessPathEntity> paths = accessPathResolverService.resolveAndPersistAccessPaths(userId);
        return ResponseEntity.ok(paths);
    }

    @PostMapping("/{userId}/access-paths/recalculate")
    public ResponseEntity<List<AccessPathEntity>> recalculateAccessPaths(@PathVariable Long userId) {
        List<AccessPathEntity> paths = accessPathResolverService.resolveAndPersistAccessPaths(userId);
        return ResponseEntity.ok(paths);
    }
}
