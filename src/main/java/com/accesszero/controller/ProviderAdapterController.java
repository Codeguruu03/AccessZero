package com.accesszero.controller;

import com.accesszero.adapter.keycloak.KeycloakAdminAdapter;
import com.accesszero.adapter.keycloak.KeycloakSessionRepresentation;
import com.accesszero.adapter.keycloak.KeycloakUserRepresentation;
import com.accesszero.adapter.ldap.LdapDirectoryAdapter;
import com.accesszero.adapter.ldap.LdapGroupRepresentation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/adapters")
public class ProviderAdapterController {

    private final KeycloakAdminAdapter keycloakAdminAdapter;
    private final LdapDirectoryAdapter ldapDirectoryAdapter;

    public ProviderAdapterController(KeycloakAdminAdapter keycloakAdminAdapter, LdapDirectoryAdapter ldapDirectoryAdapter) {
        this.keycloakAdminAdapter = keycloakAdminAdapter;
        this.ldapDirectoryAdapter = ldapDirectoryAdapter;
    }

    @GetMapping("/keycloak/users/{username}")
    public ResponseEntity<KeycloakUserRepresentation> getKeycloakUser(@PathVariable String username) {
        return ResponseEntity.ok(keycloakAdminAdapter.getUserByUsername(username));
    }

    @PostMapping("/keycloak/users/{username}/disable")
    public ResponseEntity<Map<String, Object>> disableKeycloakUser(@PathVariable String username) {
        boolean success = keycloakAdminAdapter.disableUser(username);
        return ResponseEntity.ok(Map.of("username", username, "action", "DISABLE_KEYCLOAK_USER", "success", success));
    }

    @PostMapping("/keycloak/users/{username}/logout")
    public ResponseEntity<Map<String, Object>> logoutKeycloakSessions(@PathVariable String username) {
        boolean success = keycloakAdminAdapter.logoutUserSessions(username);
        return ResponseEntity.ok(Map.of("username", username, "action", "REVOKE_KEYCLOAK_SESSIONS", "success", success));
    }

    @GetMapping("/keycloak/users/{username}/sessions")
    public ResponseEntity<List<KeycloakSessionRepresentation>> getKeycloakSessions(@PathVariable String username) {
        return ResponseEntity.ok(keycloakAdminAdapter.getUserActiveSessions(username));
    }

    @GetMapping("/ldap/users/{username}/groups")
    public ResponseEntity<List<LdapGroupRepresentation>> getLdapGroups(@PathVariable String username) {
        return ResponseEntity.ok(ldapDirectoryAdapter.getUserGroupMemberships(username));
    }

    @PostMapping("/ldap/users/{username}/quarantine")
    public ResponseEntity<Map<String, Object>> quarantineLdapUser(
            @PathVariable String username,
            @RequestBody(required = false) List<String> privilegedGroups) {
        if (privilegedGroups == null || privilegedGroups.isEmpty()) {
            privilegedGroups = List.of("finance", "payroll-admin", "vpn-users");
        }
        List<String> removed = ldapDirectoryAdapter.containLdapIdentity(username, privilegedGroups);
        return ResponseEntity.ok(Map.of(
                "username", username,
                "action", "CONTAIN_LDAP_IDENTITY",
                "removedGroups", removed,
                "addedToQuarantine", true
        ));
    }
}
