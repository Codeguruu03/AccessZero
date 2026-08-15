package com.accesszero.service;

import com.accesszero.adapter.ldap.LdapDirectoryAdapter;
import com.accesszero.adapter.ldap.LdapGroupRepresentation;
import com.accesszero.domain.entity.*;
import com.accesszero.dto.BlastRadiusDto;
import com.accesszero.dto.ContainmentSimulationDto;
import com.accesszero.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContainmentSimulationEngine {

    private static final Logger log = LoggerFactory.getLogger(ContainmentSimulationEngine.class);

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final OAuthTokenRepository oAuthTokenRepository;
    private final LdapDirectoryAdapter ldapDirectoryAdapter;
    private final BlastRadiusEngine blastRadiusEngine;

    public ContainmentSimulationEngine(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            OAuthTokenRepository oAuthTokenRepository,
            LdapDirectoryAdapter ldapDirectoryAdapter,
            BlastRadiusEngine blastRadiusEngine
    ) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.oAuthTokenRepository = oAuthTokenRepository;
        this.ldapDirectoryAdapter = ldapDirectoryAdapter;
        this.blastRadiusEngine = blastRadiusEngine;
    }

    @Transactional(readOnly = true)
    public ContainmentSimulationDto simulateContainment(Long userId) {
        log.info("Executing non-destructive Containment Simulation for User ID: {}", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // 1. Fetch Blast Radius metrics
        BlastRadiusDto blastRadius = blastRadiusEngine.calculateBlastRadius(userId);

        // 2. Fetch Sessions, Tokens, LDAP Groups
        List<UserSessionEntity> activeSessions = userSessionRepository.findByUserIdAndActive(userId, true);
        List<OAuthTokenEntity> activeTokens = oAuthTokenRepository.findByUserIdAndRevoked(userId, false);
        List<LdapGroupRepresentation> ldapGroups = ldapDirectoryAdapter.getUserGroupMemberships(user.getUsername());

        List<String> privilegedGroups = ldapGroups.stream()
                .filter(LdapGroupRepresentation::isPrivileged)
                .map(LdapGroupRepresentation::cn)
                .toList();

        // 3. Formulate Action Summary List
        List<String> actionSummary = new ArrayList<>();
        actionSummary.add(String.format("Keycloak: Set account status for user '%s' from %s -> DISABLED", user.getUsername(), user.getStatus()));
        actionSummary.add(String.format("OAuth/OIDC: Invalidate %d refresh tokens and %d access tokens across %d client apps",
                activeTokens.stream().filter(t -> t.getTokenType().name().equals("REFRESH")).count(),
                activeTokens.stream().filter(t -> t.getTokenType().name().equals("ACCESS")).count(),
                blastRadius.applicationsAffectedCount()));
        actionSummary.add(String.format("Sessions: Terminate %d active SSO sessions across Keycloak & App gateways", activeSessions.size()));
        actionSummary.add(String.format("LDAP Directory: Strip user from %d privileged groups %s", privilegedGroups.size(), privilegedGroups));
        actionSummary.add("LDAP Directory: Assign user identity to 'cn=quarantined' security group");
        actionSummary.add(String.format("Access Paths: Revoke %d total access paths (%d privileged access paths)",
                blastRadius.totalAccessPathsCount(), blastRadius.privilegedAccessPathsCount()));

        // 4. Calculate Disruption Score & Level
        int disruptionRaw = (privilegedGroups.size() * 20)
                + (blastRadius.privilegedApplicationsCount() * 25)
                + (activeSessions.size() * 5)
                + (activeTokens.size() * 2);

        int disruptionScore = Math.min(100, Math.max(0, disruptionRaw));
        String disruptionLevel = calculateDisruptionLevel(disruptionScore);
        boolean requiresApproval = disruptionScore >= 60 || !privilegedGroups.isEmpty();

        log.info("Containment Simulation generated for {}: DisruptionScore={}, DisruptionLevel={}, RequiresApproval={}",
                user.getUsername(), disruptionScore, disruptionLevel, requiresApproval);

        return new ContainmentSimulationDto(
                userId,
                user.getUsername(),
                user.getStatus().name(),
                "CONTAINED",
                activeTokens.size(),
                activeSessions.size(),
                ldapGroups.size(),
                privilegedGroups,
                blastRadius.applicationsAffectedCount(),
                disruptionLevel,
                disruptionScore,
                requiresApproval,
                actionSummary
        );
    }

    @Transactional(readOnly = true)
    public ContainmentSimulationDto simulateContainmentByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        return simulateContainment(user.getId());
    }

    private String calculateDisruptionLevel(int score) {
        if (score >= 75) return "CRITICAL";
        if (score >= 50) return "HIGH";
        if (score >= 25) return "MEDIUM";
        return "LOW";
    }
}
