package com.accesszero.service;

import com.accesszero.domain.entity.*;
import com.accesszero.domain.enums.SensitivityLevel;
import com.accesszero.dto.AffectedApplicationDto;
import com.accesszero.dto.BlastRadiusDto;
import com.accesszero.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlastRadiusEngine {

    private static final Logger log = LoggerFactory.getLogger(BlastRadiusEngine.class);

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final ApplicationRepository applicationRepository;
    private final UserSessionRepository userSessionRepository;
    private final OAuthTokenRepository oAuthTokenRepository;
    private final SAMLAssignmentRepository samlAssignmentRepository;
    private final AccessPathResolverService accessPathResolverService;

    public BlastRadiusEngine(
            UserRepository userRepository,
            GroupRepository groupRepository,
            UserGroupRepository userGroupRepository,
            ApplicationRepository applicationRepository,
            UserSessionRepository userSessionRepository,
            OAuthTokenRepository oAuthTokenRepository,
            SAMLAssignmentRepository samlAssignmentRepository,
            AccessPathResolverService accessPathResolverService
    ) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.userGroupRepository = userGroupRepository;
        this.applicationRepository = applicationRepository;
        this.userSessionRepository = userSessionRepository;
        this.oAuthTokenRepository = oAuthTokenRepository;
        this.samlAssignmentRepository = samlAssignmentRepository;
        this.accessPathResolverService = accessPathResolverService;
    }

    @Transactional(readOnly = true)
    public BlastRadiusDto calculateBlastRadius(Long userId) {
        log.info("Calculating Access Blast Radius for User ID: {}", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // 1. Sessions & Tokens
        List<UserSessionEntity> activeSessions = userSessionRepository.findByUserIdAndActive(userId, true);
        List<OAuthTokenEntity> activeTokens = oAuthTokenRepository.findByUserIdAndRevoked(userId, false);
        List<SAMLAssignmentEntity> samlAssignments = samlAssignmentRepository.findByUserId(userId);

        // 2. Groups
        List<UserGroupEntity> userGroups = userGroupRepository.findByUserId(userId);
        List<GroupEntity> groups = userGroups.stream()
                .map(ug -> groupRepository.findById(ug.getGroupId()))
                .flatMap(Optional::stream)
                .toList();

        int sensitiveGroupsCount = (int) groups.stream().filter(GroupEntity::isPrivileged).count();

        // 3. Resolved Access Paths
        List<AccessPathEntity> accessPaths = accessPathResolverService.resolveAndPersistAccessPaths(userId);
        int totalAccessPaths = accessPaths.size();
        int privilegedAccessPaths = (int) accessPaths.stream().filter(AccessPathEntity::isPrivileged).count();

        // 4. Affected Applications Analysis
        Map<Long, List<AccessPathEntity>> pathsByApp = accessPaths.stream()
                .collect(Collectors.groupingBy(AccessPathEntity::getApplicationId));

        List<AffectedApplicationDto> affectedAppDtos = new ArrayList<>();
        int privilegedAppsCount = 0;

        for (Map.Entry<Long, List<AccessPathEntity>> entry : pathsByApp.entrySet()) {
            Optional<ApplicationEntity> appOpt = applicationRepository.findById(entry.getKey());
            if (appOpt.isPresent()) {
                ApplicationEntity app = appOpt.get();
                boolean isPrivileged = app.getSensitivityLevel() == SensitivityLevel.CRITICAL || app.getSensitivityLevel() == SensitivityLevel.HIGH;
                if (isPrivileged) {
                    privilegedAppsCount++;
                }
                affectedAppDtos.add(new AffectedApplicationDto(
                        app.getId(),
                        app.getName(),
                        app.getType(),
                        app.getSensitivityLevel(),
                        isPrivileged,
                        entry.getValue().size()
                ));
            }
        }

        // 5. Risk Score Calculation Algorithm
        int rawScore = (privilegedAppsCount * 20)
                + (sensitiveGroupsCount * 15)
                + (privilegedAccessPaths * 10)
                + (activeSessions.size() * 5)
                + (activeTokens.size() * 2);

        int riskScore = Math.min(100, Math.max(0, rawScore));
        String riskLevel = calculateRiskLevel(riskScore);

        log.info("Blast Radius calculation complete for {}: RiskScore={}, RiskLevel={}, AffectedApps={}",
                user.getUsername(), riskScore, riskLevel, affectedAppDtos.size());

        return new BlastRadiusDto(
                userId,
                user.getUsername(),
                riskLevel,
                riskScore,
                activeSessions.size(),
                activeTokens.size(),
                groups.size(),
                sensitiveGroupsCount,
                affectedAppDtos.size(),
                privilegedAppsCount,
                samlAssignments.size(),
                totalAccessPaths,
                privilegedAccessPaths,
                affectedAppDtos
        );
    }

    @Transactional(readOnly = true)
    public BlastRadiusDto calculateBlastRadiusByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        return calculateBlastRadius(user.getId());
    }

    private String calculateRiskLevel(int score) {
        if (score >= 75) return "CRITICAL";
        if (score >= 50) return "HIGH";
        if (score >= 25) return "MEDIUM";
        return "LOW";
    }
}
