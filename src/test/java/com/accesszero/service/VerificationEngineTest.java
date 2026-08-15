package com.accesszero.service;

import com.accesszero.domain.entity.UserEntity;
import com.accesszero.dto.VerificationResultDto;
import com.accesszero.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VerificationEngineTest {

    @Autowired
    private VerificationEngine verificationEngine;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testVerifyZeroAccessReturnsDetailedMatrix() {
        Optional<UserEntity> userOpt = userRepository.findByUsername("rahul.sharma");
        assertTrue(userOpt.isPresent());

        UserEntity user = userOpt.get();
        VerificationResultDto result = verificationEngine.verifyContainment(1L, user.getId(), "test.verifier");

        assertNotNull(result);
        assertEquals("rahul.sharma", result.username());
        assertNotNull(result.overallStatus());
        assertNotNull(result.providerResults());
        assertTrue(result.providerResults().containsKey("KEYCLOAK"));
        assertTrue(result.providerResults().containsKey("OPENLDAP"));
        assertTrue(result.providerResults().containsKey("OAUTH_OIDC"));
        assertTrue(result.providerResults().containsKey("SAML_SSO"));

        // Validate SAML residual risk warning logic
        if (result.requiresManualActionCount() > 0) {
            assertEquals("PARTIAL", result.overallStatus());
            assertFalse(result.remainingRisks().isEmpty());
        }
    }
}
