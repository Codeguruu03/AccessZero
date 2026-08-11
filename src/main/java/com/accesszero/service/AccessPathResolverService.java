package com.accesszero.service;

import com.accesszero.domain.entity.AccessPathEntity;
import com.accesszero.domain.graph.GraphNode;
import com.accesszero.domain.graph.IdentityGraph;
import com.accesszero.domain.graph.NodeType;
import com.accesszero.domain.enums.PathType;
import com.accesszero.repository.AccessPathRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccessPathResolverService {

    private static final Logger log = LoggerFactory.getLogger(AccessPathResolverService.class);

    private final IdentityGraphEngine identityGraphEngine;
    private final AccessPathRepository accessPathRepository;

    public AccessPathResolverService(IdentityGraphEngine identityGraphEngine, AccessPathRepository accessPathRepository) {
        this.identityGraphEngine = identityGraphEngine;
        this.accessPathRepository = accessPathRepository;
    }

    @Transactional
    public List<AccessPathEntity> resolveAndPersistAccessPaths(Long userId) {
        log.info("Resolving all directed access paths for User ID: {}", userId);
        IdentityGraph graph = identityGraphEngine.buildGraphForUser(userId);
        String userStartNodeId = "USER:" + userId;

        List<List<GraphNode>> rawPaths = graph.findAllPathsToType(userStartNodeId, NodeType.APPLICATION);
        log.info("Discovered {} effective access paths for User ID: {}", rawPaths.size(), userId);

        accessPathRepository.deleteByUserId(userId);
        List<AccessPathEntity> pathEntities = new ArrayList<>();

        for (List<GraphNode> path : rawPaths) {
            String pathDescription = path.stream()
                    .map(GraphNode::getLabel)
                    .collect(Collectors.joining(" -> "));

            GraphNode appNode = path.get(path.size() - 1);
            Long appId = Long.parseLong(appNode.getId().replace("APP:", ""));

            PathType pathType = determinePathType(path);
            boolean isPrivileged = path.stream().anyMatch(GraphNode::isPrivileged);

            AccessPathEntity entity = new AccessPathEntity(
                    userId,
                    appId,
                    pathDescription,
                    pathType,
                    isPrivileged,
                    false
            );
            pathEntities.add(accessPathRepository.save(entity));
        }

        return pathEntities;
    }

    private PathType determinePathType(List<GraphNode> path) {
        for (GraphNode node : path) {
            if (node.getType() == NodeType.GROUP) return PathType.GROUP_INHERITED;
            if (node.getType() == NodeType.TOKEN) return PathType.TOKEN_BASED;
            if (node.getType() == NodeType.SAML_ASSIGNMENT) return PathType.SAML_ASSIGNED;
        }
        return PathType.DIRECT;
    }
}
