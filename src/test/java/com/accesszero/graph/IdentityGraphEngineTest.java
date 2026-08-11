package com.accesszero.graph;

import com.accesszero.domain.entity.UserEntity;
import com.accesszero.domain.graph.GraphNode;
import com.accesszero.domain.graph.IdentityGraph;
import com.accesszero.domain.graph.NodeType;
import com.accesszero.repository.UserRepository;
import com.accesszero.service.IdentityGraphEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IdentityGraphEngineTest {

    @Autowired
    private IdentityGraphEngine identityGraphEngine;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testBuildGraphForUser() {
        Optional<UserEntity> userOpt = userRepository.findByUsername("rahul.sharma");
        assertTrue(userOpt.isPresent());

        UserEntity user = userOpt.get();
        IdentityGraph graph = identityGraphEngine.buildGraphForUser(user.getId());

        assertNotNull(graph);
        assertTrue(graph.getAllNodes().size() > 5);

        GraphNode userNode = graph.getNode("USER:" + user.getId());
        assertNotNull(userNode);
        assertEquals("rahul.sharma", userNode.getLabel());
        assertEquals(NodeType.USER, userNode.getType());

        List<List<GraphNode>> appPaths = graph.findAllPathsToType("USER:" + user.getId(), NodeType.APPLICATION);
        assertNotNull(appPaths);
        assertFalse(appPaths.isEmpty());
    }
}
