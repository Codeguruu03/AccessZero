package com.accesszero.service;

import com.accesszero.domain.entity.*;
import com.accesszero.domain.graph.*;
import com.accesszero.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IdentityGraphEngine {

    private static final Logger log = LoggerFactory.getLogger(IdentityGraphEngine.class);

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final ApplicationRepository applicationRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupRoleRepository groupRoleRepository;
    private final UserSessionRepository userSessionRepository;
    private final OAuthTokenRepository oAuthTokenRepository;
    private final SAMLAssignmentRepository samlAssignmentRepository;

    public IdentityGraphEngine(
            UserRepository userRepository,
            GroupRepository groupRepository,
            RoleRepository roleRepository,
            ApplicationRepository applicationRepository,
            UserGroupRepository userGroupRepository,
            GroupRoleRepository groupRoleRepository,
            UserSessionRepository userSessionRepository,
            OAuthTokenRepository oAuthTokenRepository,
            SAMLAssignmentRepository samlAssignmentRepository
    ) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.roleRepository = roleRepository;
        this.applicationRepository = applicationRepository;
        this.userGroupRepository = userGroupRepository;
        this.groupRoleRepository = groupRoleRepository;
        this.userSessionRepository = userSessionRepository;
        this.oAuthTokenRepository = oAuthTokenRepository;
        this.samlAssignmentRepository = samlAssignmentRepository;
    }

    public IdentityGraph buildGraphForUser(Long userId) {
        log.info("Constructing Identity Graph for User ID: {}", userId);
        IdentityGraph graph = new IdentityGraph();

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User ID [{}] not found for graph construction", userId);
            return graph;
        }

        UserEntity user = userOpt.get();
        String userNodeId = "USER:" + user.getId();
        GraphNode userNode = new GraphNode(userNodeId, user.getUsername(), NodeType.USER, false);
        graph.addNode(userNode);

        // 1. Group Memberships & Roles
        List<UserGroupEntity> userGroups = userGroupRepository.findByUserId(userId);
        for (UserGroupEntity ug : userGroups) {
            Optional<GroupEntity> groupOpt = groupRepository.findById(ug.getGroupId());
            if (groupOpt.isPresent()) {
                GroupEntity group = groupOpt.get();
                String groupNodeId = "GROUP:" + group.getId();
                GraphNode groupNode = new GraphNode(groupNodeId, group.getName(), NodeType.GROUP, group.isPrivileged());
                graph.addNode(groupNode);
                graph.addEdge(new GraphEdge(userNodeId, groupNodeId, EdgeType.MEMBER_OF, "Member of " + group.getName()));

                // Group -> Roles
                List<GroupRoleEntity> groupRoles = groupRoleRepository.findByGroupId(group.getId());
                for (GroupRoleEntity gr : groupRoles) {
                    Optional<RoleEntity> roleOpt = roleRepository.findById(gr.getRoleId());
                    if (roleOpt.isPresent()) {
                        RoleEntity role = roleOpt.get();
                        String roleNodeId = "ROLE:" + role.getId();
                        GraphNode roleNode = new GraphNode(roleNodeId, role.getName(), NodeType.ROLE, group.isPrivileged());
                        graph.addNode(roleNode);
                        graph.addEdge(new GraphEdge(groupNodeId, roleNodeId, EdgeType.HAS_ROLE, "Inherits role " + role.getName()));

                        // Role -> Applications
                        List<ApplicationEntity> allApps = applicationRepository.findAll();
                        for (ApplicationEntity app : allApps) {
                            String appNodeId = "APP:" + app.getId();
                            GraphNode appNode = new GraphNode(appNodeId, app.getName(), NodeType.APPLICATION, app.getSensitivityLevel().name().equals("CRITICAL"));
                            graph.addNode(appNode);
                            graph.addEdge(new GraphEdge(roleNodeId, appNodeId, EdgeType.TARGETS_APP, "Grants access to " + app.getName()));
                        }
                    }
                }
            }
        }

        // 2. Active User Sessions
        List<UserSessionEntity> activeSessions = userSessionRepository.findByUserIdAndActive(userId, true);
        for (UserSessionEntity session : activeSessions) {
            String sessionNodeId = "SESSION:" + session.getId();
            GraphNode sessionNode = new GraphNode(sessionNodeId, "Session: " + session.getSessionId(), NodeType.SESSION, false);
            graph.addNode(sessionNode);
            graph.addEdge(new GraphEdge(userNodeId, sessionNodeId, EdgeType.SESSION_ACCESS, "Active Session"));

            // Connect session to sample applications
            List<ApplicationEntity> apps = applicationRepository.findAll();
            if (!apps.isEmpty()) {
                ApplicationEntity app = apps.get((int) (session.getId() % apps.size()));
                String appNodeId = "APP:" + app.getId();
                GraphNode appNode = new GraphNode(appNodeId, app.getName(), NodeType.APPLICATION, app.getSensitivityLevel().name().equals("CRITICAL"));
                graph.addNode(appNode);
                graph.addEdge(new GraphEdge(sessionNodeId, appNodeId, EdgeType.TARGETS_APP, "Session grants app access"));
            }
        }

        // 3. OAuth Tokens
        List<OAuthTokenEntity> activeTokens = oAuthTokenRepository.findByUserIdAndRevoked(userId, false);
        for (OAuthTokenEntity token : activeTokens) {
            String tokenNodeId = "TOKEN:" + token.getId();
            GraphNode tokenNode = new GraphNode(tokenNodeId, "OAuth " + token.getTokenType() + " Token", NodeType.TOKEN, false);
            graph.addNode(tokenNode);
            graph.addEdge(new GraphEdge(userNodeId, tokenNodeId, EdgeType.TOKEN_ACCESS, "OAuth Token"));

            List<ApplicationEntity> apps = applicationRepository.findAll();
            if (!apps.isEmpty()) {
                ApplicationEntity app = apps.get((int) (token.getId() % apps.size()));
                String appNodeId = "APP:" + app.getId();
                GraphNode appNode = new GraphNode(appNodeId, app.getName(), NodeType.APPLICATION, app.getSensitivityLevel().name().equals("CRITICAL"));
                graph.addNode(appNode);
                graph.addEdge(new GraphEdge(tokenNodeId, appNodeId, EdgeType.TARGETS_APP, "OAuth token grants access"));
            }
        }

        // 4. SAML Assignments
        List<SAMLAssignmentEntity> samlAssignments = samlAssignmentRepository.findByUserId(userId);
        for (SAMLAssignmentEntity saml : samlAssignments) {
            Optional<ApplicationEntity> appOpt = applicationRepository.findById(saml.getApplicationId());
            if (appOpt.isPresent()) {
                ApplicationEntity app = appOpt.get();
                String samlNodeId = "SAML:" + saml.getId();
                GraphNode samlNode = new GraphNode(samlNodeId, "SAML SSO: " + app.getName(), NodeType.SAML_ASSIGNMENT, false);
                graph.addNode(samlNode);
                graph.addEdge(new GraphEdge(userNodeId, samlNodeId, EdgeType.SAML_ACCESS, "SAML Assignment"));

                String appNodeId = "APP:" + app.getId();
                GraphNode appNode = new GraphNode(appNodeId, app.getName(), NodeType.APPLICATION, app.getSensitivityLevel().name().equals("CRITICAL"));
                graph.addNode(appNode);
                graph.addEdge(new GraphEdge(samlNodeId, appNodeId, EdgeType.TARGETS_APP, "SAML SSO Access"));
            }
        }

        log.info("Completed Identity Graph construction for {}: {} nodes resolved", user.getUsername(), graph.getAllNodes().size());
        return graph;
    }
}
