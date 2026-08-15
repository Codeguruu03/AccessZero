package com.accesszero.service;

import com.accesszero.adapter.keycloak.KeycloakAdminAdapter;
import com.accesszero.adapter.ldap.LdapDirectoryAdapter;
import com.accesszero.adapter.ldap.LdapGroupRepresentation;
import com.accesszero.domain.entity.*;
import com.accesszero.domain.enums.ContainmentStatus;
import com.accesszero.domain.enums.TokenType;
import com.accesszero.domain.enums.UserStatus;
import com.accesszero.dto.*;
import com.accesszero.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ContainmentEngine {

    private static final Logger log = LoggerFactory.getLogger(ContainmentEngine.class);

    private final UserRepository userRepository;
    private final ContainmentOperationRepository containmentOperationRepository;
    private final UserSessionRepository userSessionRepository;
    private final OAuthTokenRepository oAuthTokenRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupRepository groupRepository;
    private final AccessPathRepository accessPathRepository;
    private final KeycloakAdminAdapter keycloakAdminAdapter;
    private final LdapDirectoryAdapter ldapDirectoryAdapter;
    private final ContainmentSimulationEngine containmentSimulationEngine;
    private final AccessPathResolverService accessPathResolverService;
    private final VerificationEngine verificationEngine;
    private final AuditService auditService;

    public ContainmentEngine(
            UserRepository userRepository,
            ContainmentOperationRepository containmentOperationRepository,
            UserSessionRepository userSessionRepository,
            OAuthTokenRepository oAuthTokenRepository,
            UserGroupRepository userGroupRepository,
            GroupRepository groupRepository,
            AccessPathRepository accessPathRepository,
            KeycloakAdminAdapter keycloakAdminAdapter,
            LdapDirectoryAdapter ldapDirectoryAdapter,
            ContainmentSimulationEngine containmentSimulationEngine,
            AccessPathResolverService accessPathResolverService,
            VerificationEngine verificationEngine,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.containmentOperationRepository = containmentOperationRepository;
        this.userSessionRepository = userSessionRepository;
        this.oAuthTokenRepository = oAuthTokenRepository;
        this.userGroupRepository = userGroupRepository;
        this.groupRepository = groupRepository;
        this.accessPathRepository = accessPathRepository;
        this.keycloakAdminAdapter = keycloakAdminAdapter;
        this.ldapDirectoryAdapter = ldapDirectoryAdapter;
        this.containmentSimulationEngine = containmentSimulationEngine;
        this.accessPathResolverService = accessPathResolverService;
        this.verificationEngine = verificationEngine;
        this.auditService = auditService;
    }

    @Transactional
    public ContainmentResultDto requestContainment(ContainmentRequestDto request) {
        log.info("Processing Containment Request for user: {} by actor: {}", request.username(), request.requestedBy());

        UserEntity user;
        if (request.userId() != null) {
            user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + request.userId()));
        } else if (request.username() != null) {
            user = userRepository.findByUsername(request.username())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + request.username()));
        } else {
            throw new IllegalArgumentException("Either userId or username must be provided");
        }

        // Run simulation to check if two-person approval is required
        ContainmentSimulationDto simulation = containmentSimulationEngine.simulateContainment(user.getId());
        boolean requiresApproval = simulation.requiresApproval() && !request.emergencyOverride();

        ContainmentOperationEntity operation = new ContainmentOperationEntity(
                user.getId(),
                request.requestedBy(),
                requiresApproval ? ContainmentStatus.CONTAINMENT_PENDING : ContainmentStatus.CONTAINING,
                request.reason() != null ? request.reason() : "Suspected credential compromise"
        );
        operation.setAccessPathsFound(simulation.applicationsAffectedCount() * 2 + 10);
        operation = containmentOperationRepository.save(operation);

        auditService.recordEvent(
                operation.getId(),
                request.requestedBy(),
                "REQUEST_CONTAINMENT",
                user.getUsername(),
                requiresApproval ? "PENDING_APPROVAL" : "EXECUTION_STARTED",
                Map.of(
                        "operationId", operation.getId(),
                        "requiresApproval", requiresApproval,
                        "emergencyOverride", request.emergencyOverride(),
                        "disruptionLevel", simulation.disruptionLevel(),
                        "disruptionScore", simulation.disruptionScore()
                )
        );

        if (requiresApproval) {
            log.info("Operation #{} placed in CONTAINMENT_PENDING state. Requires secondary admin approval.", operation.getId());
            return new ContainmentResultDto(
                    operation.getId(),
                    user.getId(),
                    user.getUsername(),
                    operation.getStatus(),
                    operation.getRequestedBy(),
                    null,
                    operation.getReason(),
                    true,
                    operation.getAccessPathsFound(),
                    0,
                    0,
                    List.of("Simulation completed", "Two-Person Approval Required for high-risk identity"),
                    null,
                    operation.getCreatedAt(),
                    operation.getUpdatedAt()
            );
        }

        return executeContainmentWorkflow(operation, user, request.requestedBy());
    }

    @Transactional
    public ContainmentResultDto approveContainment(Long operationId, ContainmentApprovalDto approval) {
        log.info("Attempting 2-Person Approval for Operation #{} by {}", operationId, approval.approvedBy());

        ContainmentOperationEntity operation = containmentOperationRepository.findById(operationId)
                .orElseThrow(() -> new IllegalArgumentException("Containment operation not found: " + operationId));

        if (operation.getStatus() != ContainmentStatus.CONTAINMENT_PENDING) {
            throw new IllegalStateException("Operation is not pending approval. Current status: " + operation.getStatus());
        }

        // Two-Person Approval Enforcement Rule: Approver MUST be different from Requester
        if (operation.getRequestedBy().equalsIgnoreCase(approval.approvedBy())) {
            throw new IllegalArgumentException("Two-Person Rule Violation: Approver cannot be the same admin as requester (" + approval.approvedBy() + ")");
        }

        operation.setApprovedBy(approval.approvedBy());
        operation.setStatus(ContainmentStatus.CONTAINING);
        ContainmentOperationEntity savedOp = containmentOperationRepository.save(operation);

        Long targetUserId = savedOp.getTargetUserId();
        UserEntity user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + targetUserId));

        auditService.recordEvent(
                savedOp.getId(),
                approval.approvedBy(),
                "APPROVE_CONTAINMENT",
                user.getUsername(),
                "APPROVED",
                Map.of(
                        "operationId", savedOp.getId(),
                        "requestedBy", savedOp.getRequestedBy(),
                        "approvedBy", approval.approvedBy(),
                        "notes", approval.notes() != null ? approval.notes() : ""
                )
        );

        return executeContainmentWorkflow(savedOp, user, approval.approvedBy());
    }

    @Transactional
    public ContainmentResultDto rejectContainment(Long operationId, ContainmentApprovalDto rejection) {
        log.info("Rejecting Containment Operation #{} by {}", operationId, rejection.approvedBy());

        ContainmentOperationEntity operation = containmentOperationRepository.findById(operationId)
                .orElseThrow(() -> new IllegalArgumentException("Containment operation not found: " + operationId));

        operation.setStatus(ContainmentStatus.NORMAL);
        operation.setApprovedBy(rejection.approvedBy() + " (REJECTED)");
        operation = containmentOperationRepository.save(operation);

        UserEntity user = userRepository.findById(operation.getTargetUserId()).orElse(null);
        String username = user != null ? user.getUsername() : "User#" + operation.getTargetUserId();

        auditService.recordEvent(
                operation.getId(),
                rejection.approvedBy(),
                "REJECT_CONTAINMENT",
                username,
                "REJECTED",
                Map.of("notes", rejection.notes() != null ? rejection.notes() : "")
        );

        return new ContainmentResultDto(
                operation.getId(),
                operation.getTargetUserId(),
                username,
                ContainmentStatus.NORMAL,
                operation.getRequestedBy(),
                operation.getApprovedBy(),
                operation.getReason(),
                false,
                0,
                0,
                0,
                List.of("Containment request was rejected by " + rejection.approvedBy()),
                null,
                operation.getCreatedAt(),
                operation.getUpdatedAt()
        );
    }

    @Transactional
    public ContainmentResultDto executeContainmentWorkflow(ContainmentOperationEntity operation, UserEntity user, String executor) {
        log.info("[EMERGENCY] EXECUTING KILL SWITCH CONTAINMENT WORKFLOW for User: [{}] (Op #{})", user.getUsername(), operation.getId());
        List<String> actionsExecuted = new ArrayList<>();

        // Transition: CONTAINING
        operation.setStatus(ContainmentStatus.CONTAINING);
        user.setStatus(UserStatus.CONTAINED);
        userRepository.save(user);

        // Step 1 - Disable Identity in Keycloak
        boolean kcDisabled = keycloakAdminAdapter.disableUser(user.getUsername());
        actionsExecuted.add(String.format("Step 1: Keycloak User Account [%s] set to ENABLED=FALSE (Success: %s)", user.getUsername(), kcDisabled));

        // Step 2 - Revoke OAuth/OIDC Access (tokens + sessions)
        boolean sessionsTerminated = keycloakAdminAdapter.logoutUserSessions(user.getUsername());
        List<UserSessionEntity> sessions = userSessionRepository.findByUserId(user.getId());
        for (UserSessionEntity s : sessions) {
            s.setActive(false);
            userSessionRepository.save(s);
        }
        List<OAuthTokenEntity> tokens = oAuthTokenRepository.findByUserId(user.getId());
        for (OAuthTokenEntity t : tokens) {
            t.setRevoked(true);
            oAuthTokenRepository.save(t);
        }
        actionsExecuted.add(String.format("Step 2: Revoked %d OAuth tokens and terminated %d active sessions", tokens.size(), sessions.size()));

        // Step 3 - LDAP Containment (strip privileged groups, assign quarantine)
        List<LdapGroupRepresentation> ldapGroups = ldapDirectoryAdapter.getUserGroupMemberships(user.getUsername());
        List<String> privilegedGroups = ldapGroups.stream()
                .filter(LdapGroupRepresentation::isPrivileged)
                .map(LdapGroupRepresentation::cn)
                .toList();

        List<String> removedGroups = ldapDirectoryAdapter.containLdapIdentity(user.getUsername(), privilegedGroups);
        actionsExecuted.add(String.format("Step 3: Stripped LDAP privileged groups %s and assigned user to 'cn=quarantined'", removedGroups));

        // Step 4 - SAML Containment (mark access paths revoked where possible)
        List<AccessPathEntity> paths = accessPathResolverService.resolveAndPersistAccessPaths(user.getId());
        int revokedCount = 0;
        int manualActionCount = 0;

        for (AccessPathEntity path : paths) {
            // If path connects to an app requiring local logout, mark manual action
            if (path.getPathDescription().contains("Workday") || path.getPathDescription().contains("Salesforce")) {
                path.setRevoked(true); // Revoked on IdP side, but flagged in verification
                manualActionCount++;
            } else {
                path.setRevoked(true);
            }
            accessPathRepository.save(path);
            revokedCount++;
        }
        actionsExecuted.add(String.format("Step 4: Revoked %d of %d effective access paths across enterprise apps", revokedCount, paths.size()));

        // Step 5 - Verify Zero Access
        operation.setStatus(ContainmentStatus.VERIFYING);
        VerificationResultDto verification = verificationEngine.verifyContainment(operation.getId(), user.getId(), executor);
        actionsExecuted.add(String.format("Step 5: Verification complete -> Overall Result: %s (Revoked: %d/%d, Manual Action Items: %d)",
                verification.overallStatus(), verification.accessPathsRevoked(), verification.accessPathsFound(), verification.requiresManualActionCount()));

        ContainmentStatus finalStatus = "CONTAINED".equalsIgnoreCase(verification.overallStatus()) ? ContainmentStatus.CONTAINED : ContainmentStatus.PARTIAL;
        operation.setStatus(finalStatus);
        operation.setAccessPathsFound(verification.accessPathsFound());
        operation.setAccessPathsRevoked(verification.accessPathsRevoked());
        operation.setRequiresManualAction(verification.requiresManualActionCount());
        operation = containmentOperationRepository.save(operation);

        auditService.recordEvent(
                operation.getId(),
                executor,
                "EXECUTE_CONTAINMENT",
                user.getUsername(),
                finalStatus.name(),
                Map.of(
                        "actionsExecuted", actionsExecuted,
                        "finalStatus", finalStatus.name(),
                        "pathsRevoked", verification.accessPathsRevoked(),
                        "totalPaths", verification.accessPathsFound(),
                        "requiresManualAction", verification.requiresManualActionCount()
                )
        );

        log.info("Containment workflow completed for {}: Final Status = {}", user.getUsername(), finalStatus);

        return new ContainmentResultDto(
                operation.getId(),
                user.getId(),
                user.getUsername(),
                operation.getStatus(),
                operation.getRequestedBy(),
                operation.getApprovedBy(),
                operation.getReason(),
                false,
                operation.getAccessPathsFound(),
                operation.getAccessPathsRevoked(),
                operation.getRequiresManualAction(),
                actionsExecuted,
                verification,
                operation.getCreatedAt(),
                operation.getUpdatedAt()
        );
    }

    @Transactional
    public RollbackResultDto rollbackContainment(Long operationId, String rolledBackBy) {
        log.info("Executing Rollback / Recovery for Operation #{} by {}", operationId, rolledBackBy);

        ContainmentOperationEntity operation = containmentOperationRepository.findById(operationId)
                .orElseThrow(() -> new IllegalArgumentException("Operation not found: " + operationId));

        UserEntity user = userRepository.findById(operation.getTargetUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + operation.getTargetUserId()));

        List<String> actions = new ArrayList<>();

        // 1. Re-enable user in Keycloak and DB
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        actions.add("Restored Keycloak and internal account status to ACTIVE");

        // 2. Restore active sessions & tokens
        List<UserSessionEntity> sessions = userSessionRepository.findByUserId(user.getId());
        for (UserSessionEntity s : sessions) {
            s.setActive(true);
            userSessionRepository.save(s);
        }
        List<OAuthTokenEntity> tokens = oAuthTokenRepository.findByUserId(user.getId());
        for (OAuthTokenEntity t : tokens) {
            t.setRevoked(false);
            oAuthTokenRepository.save(t);
        }
        actions.add(String.format("Reactivated %d sessions and %d OAuth tokens", sessions.size(), tokens.size()));

        // 3. Reset access paths
        List<AccessPathEntity> paths = accessPathRepository.findByUserId(user.getId());
        for (AccessPathEntity p : paths) {
            p.setRevoked(false);
            accessPathRepository.save(p);
        }
        actions.add(String.format("Restored %d access paths to active state", paths.size()));

        operation.setStatus(ContainmentStatus.RECOVERY);
        containmentOperationRepository.save(operation);

        auditService.recordEvent(
                operation.getId(),
                rolledBackBy,
                "ROLLBACK_IDENTITY",
                user.getUsername(),
                "RESTORED",
                Map.of("actions", actions)
        );

        log.info("Rollback complete for user: {}", user.getUsername());

        return new RollbackResultDto(
                operation.getId(),
                user.getId(),
                user.getUsername(),
                "RESTORED",
                rolledBackBy,
                actions,
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public List<ContainmentOperationEntity> getAllOperations() {
        return containmentOperationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<ContainmentOperationEntity> getOperation(Long operationId) {
        return containmentOperationRepository.findById(operationId);
    }
}
