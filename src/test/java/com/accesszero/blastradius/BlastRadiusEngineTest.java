package com.accesszero.blastradius;

import com.accesszero.domain.entity.UserEntity;
import com.accesszero.domain.enums.UserStatus;
import com.accesszero.dto.BlastRadiusDto;
import com.accesszero.repository.UserRepository;
import com.accesszero.service.BlastRadiusEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BlastRadiusEngineTest {

    @Autowired
    private BlastRadiusEngine blastRadiusEngine;

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
    void testCalculateBlastRadiusForRahul() {
        Optional<UserEntity> userOpt = userRepository.findByUsername("rahul.sharma");
        assertTrue(userOpt.isPresent());

        UserEntity user = userOpt.get();
        BlastRadiusDto blastRadius = blastRadiusEngine.calculateBlastRadius(user.getId());

        assertNotNull(blastRadius);
        assertEquals("rahul.sharma", blastRadius.username());
        assertEquals(7, blastRadius.activeSessionsCount());
        assertEquals(14, blastRadius.oauthTokensCount());
        assertTrue(blastRadius.sensitiveGroupsCount() > 0);
        assertTrue(blastRadius.totalAccessPathsCount() > 0);
        assertTrue(blastRadius.riskScore() >= 75, "Rahul should have CRITICAL risk score due to privileged groups and active sessions");
        assertEquals("CRITICAL", blastRadius.riskLevel());
        assertFalse(blastRadius.affectedApplications().isEmpty());
    }

    @Test
    void testCalculateBlastRadiusByUsername() {
        BlastRadiusDto blastRadius = blastRadiusEngine.calculateBlastRadiusByUsername("rahul.sharma");
        assertNotNull(blastRadius);
        assertEquals("rahul.sharma", blastRadius.username());
        assertEquals("CRITICAL", blastRadius.riskLevel());
    }
}
