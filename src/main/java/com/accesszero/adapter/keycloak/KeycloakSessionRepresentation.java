package com.accesszero.adapter.keycloak;

import java.time.LocalDateTime;

public record KeycloakSessionRepresentation(
        String id,
        String userId,
        String username,
        String ipAddress,
        LocalDateTime start,
        LocalDateTime lastAccess
) {}
