package com.accesszero.adapter;

import com.accesszero.adapter.keycloak.KeycloakAdminAdapter;
import com.accesszero.adapter.keycloak.KeycloakSessionRepresentation;
import com.accesszero.adapter.keycloak.KeycloakUserRepresentation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class KeycloakAdminAdapterTest {

    @Autowired
    private KeycloakAdminAdapter keycloakAdminAdapter;

    @Test
    void testGetUserByUsername() {
        KeycloakUserRepresentation user = keycloakAdminAdapter.getUserByUsername("rahul.sharma");
        assertNotNull(user);
        assertEquals("rahul.sharma", user.username());
        assertTrue(user.enabled());
    }

    @Test
    void testDisableUser() {
        boolean disabled = keycloakAdminAdapter.disableUser("rahul.sharma");
        assertTrue(disabled);
    }

    @Test
    void testLogoutUserSessions() {
        boolean loggedOut = keycloakAdminAdapter.logoutUserSessions("rahul.sharma");
        assertTrue(loggedOut);
    }

    @Test
    void testGetUserActiveSessions() {
        List<KeycloakSessionRepresentation> sessions = keycloakAdminAdapter.getUserActiveSessions("rahul.sharma");
        assertNotNull(sessions);
        assertEquals(7, sessions.size());
    }
}
