package com.accesszero.adapter.keycloak;

public record KeycloakUserRepresentation(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled
) {}
