package com.accesszero.service;

import com.accesszero.domain.entity.UserEntity;
import com.accesszero.domain.enums.UserStatus;
import com.accesszero.dto.VerificationResultDto;
import com.accesszero.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
        userRepository.findByUsername("rahul.sharma").ifPresent(u -> {
            u.setStatus(UserStatus.ACTIVE);
            userRepository.save(u);
        });
    }

    @Test
    void testVerifyZeroAccessReturnsDetailedMatrix() {
        Optional<UserEntity> userOpt = userRepository.findByUsername("rahul.sharma");
        assertTrue(userOpt.isPresent());

        UserEntity user = userOpt.get();

        // 1. When user is active/uncontained -> verification detects uncontained state
        VerificationResultDto preResult = verificationEngine.verifyContainment(1L, user.getId(), "test.verifier");
        assertNotNull(preResult);
        assertEquals("rahul.sharma", preResult.username());
        assertNotNull(preResult.overallStatus());
        assertNotNull(preResult.providerResults());
        assertTrue(preResult.providerResults().containsKey("KEYCLOAK"));
        assertTrue(preResult.providerResults().containsKey("OPENLDAP"));
        assertTrue(preResult.providerResults().containsKey("OAUTH_OIDC"));
        assertTrue(preResult.providerResults().containsKey("SAML_SSO"));

        // 2. When user is set to CONTAINED -> verification evaluates SAML residual risks
        user.setStatus(UserStatus.CONTAINED);
        userRepository.save(user);

        VerificationResultDto postResult = verificationEngine.verifyContainment(1L, user.getId(), "test.verifier");
        assertNotNull(postResult);
        assertEquals("PARTIAL", postResult.overallStatus());
        assertTrue(postResult.requiresManualActionCount() > 0);
        assertFalse(postResult.remainingRisks().isEmpty());

        // Restore user to active
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }
}
