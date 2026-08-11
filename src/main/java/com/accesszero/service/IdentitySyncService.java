package com.accesszero.service;

import com.accesszero.adapter.keycloak.KeycloakAdminAdapter;
import com.accesszero.adapter.keycloak.KeycloakSessionRepresentation;
import com.accesszero.adapter.keycloak.KeycloakUserRepresentation;
import com.accesszero.adapter.ldap.LdapDirectoryAdapter;
import com.accesszero.adapter.ldap.LdapGroupRepresentation;
import com.accesszero.domain.entity.*;
import com.accesszero.domain.enums.GroupType;
import com.accesszero.domain.enums.UserStatus;
import com.accesszero.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IdentitySyncService {

    private static final Logger log = LoggerFactory.getLogger(IdentitySyncService.class);

    private final KeycloakAdminAdapter keycloakAdminAdapter;
    private final LdapDirectoryAdapter ldapDirectoryAdapter;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserSessionRepository userSessionRepository;

    public IdentitySyncService(
            KeycloakAdminAdapter keycloakAdminAdapter,
            LdapDirectoryAdapter ldapDirectoryAdapter,
            UserRepository userRepository,
            GroupRepository groupRepository,
            UserGroupRepository userGroupRepository,
            UserSessionRepository userSessionRepository
    ) {
        this.keycloakAdminAdapter = keycloakAdminAdapter;
        this.ldapDirectoryAdapter = ldapDirectoryAdapter;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.userGroupRepository = userGroupRepository;
        this.userSessionRepository = userSessionRepository;
    }

    @Transactional
    public Map<String, Object> syncIdentity(String username) {
        log.info("Initiating identity synchronization from Keycloak and LDAP for username: {}", username);

        // 1. Sync Keycloak User Representation
        KeycloakUserRepresentation kcUser = keycloakAdminAdapter.getUserByUsername(username);
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseGet(() -> new UserEntity(username, kcUser.email(), kcUser.firstName(), kcUser.lastName(), "Finance", UserStatus.ACTIVE));

        userEntity.setEmail(kcUser.email());
        userEntity.setFirstName(kcUser.firstName());
        userEntity.setLastName(kcUser.lastName());
        userEntity.setStatus(kcUser.enabled() ? UserStatus.ACTIVE : UserStatus.CONTAINED);
        userEntity = userRepository.save(userEntity);

        // 2. Sync LDAP Groups
        List<LdapGroupRepresentation> ldapGroups = ldapDirectoryAdapter.getUserGroupMemberships(username);
        int syncedGroupsCount = 0;
        for (LdapGroupRepresentation ldapGroup : ldapGroups) {
            final UserEntity targetUser = userEntity;
            GroupEntity groupEntity = groupRepository.findByName(ldapGroup.cn())
                    .orElseGet(() -> groupRepository.save(new GroupEntity(
                            ldapGroup.cn(),
                            "LDAP imported group " + ldapGroup.cn(),
                            ldapGroup.isPrivileged(),
                            GroupType.LDAP
                    )));

            boolean exists = userGroupRepository.findByUserId(targetUser.getId())
                    .stream()
                    .anyMatch(ug -> ug.getGroupId().equals(groupEntity.getId()));

            if (!exists) {
                userGroupRepository.save(new UserGroupEntity(targetUser.getId(), groupEntity.getId()));
                syncedGroupsCount++;
            }
        }

        // 3. Sync Active Sessions
        List<KeycloakSessionRepresentation> kcSessions = keycloakAdminAdapter.getUserActiveSessions(username);
        final UserEntity targetUser = userEntity;

        for (KeycloakSessionRepresentation kcSession : kcSessions) {
            boolean sessionExists = userSessionRepository.findByUserId(targetUser.getId())
                    .stream()
                    .anyMatch(s -> s.getSessionId().equals(kcSession.id()));

            if (!sessionExists) {
                userSessionRepository.save(new UserSessionEntity(
                        targetUser.getId(),
                        kcSession.id(),
                        "Keycloak-OIDC",
                        kcSession.ipAddress(),
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                        true,
                        LocalDateTime.now().plusHours(8)
                ));
            }
        }

        Map<String, Object> syncResult = new HashMap<>();
        syncResult.put("username", username);
        syncResult.put("status", "SUCCESS");
        syncResult.put("keycloakSynced", true);
        syncResult.put("ldapGroupsSynced", ldapGroups.size());
        syncResult.put("newGroupAssignments", syncedGroupsCount);
        syncResult.put("activeSessionsSynced", kcSessions.size());

        log.info("Identity synchronization complete for {}: {}", username, syncResult);
        return syncResult;
    }
}
