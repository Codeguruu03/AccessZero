package com.accesszero.simulation;

import com.accesszero.domain.entity.UserEntity;
import com.accesszero.domain.enums.UserStatus;
import com.accesszero.dto.ContainmentSimulationDto;
import com.accesszero.repository.UserRepository;
import com.accesszero.service.ContainmentSimulationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ContainmentSimulationEngineTest {

    @Autowired
    private ContainmentSimulationEngine containmentSimulationEngine;

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
    void testSimulateContainmentForRahul() {
        Optional<UserEntity> userOpt = userRepository.findByUsername("rahul.sharma");
        assertTrue(userOpt.isPresent());

        UserEntity user = userOpt.get();
        ContainmentSimulationDto simulation = containmentSimulationEngine.simulateContainment(user.getId());

        assertNotNull(simulation);
        assertEquals("rahul.sharma", simulation.username());
        assertEquals("ACTIVE", simulation.accountStatusCurrent());
        assertEquals("CONTAINED", simulation.accountStatusSimulated());
        assertTrue(simulation.tokensToRevokeCount() >= 14, "Should have at least 14 tokens to revoke");
        assertTrue(simulation.sessionsToTerminateCount() >= 7, "Should have at least 7 active sessions to terminate");
        assertFalse(simulation.privilegedLdapGroupsToRemove().isEmpty());
        assertTrue(simulation.privilegedLdapGroupsToRemove().contains("payroll-admin"));
        assertTrue(simulation.requiresApproval(), "Disruption score for Rahul should require multi-admin approval");
        assertFalse(simulation.simulatedActionSummary().isEmpty());
    }

    @Test
    void testSimulateContainmentByUsername() {
        ContainmentSimulationDto simulation = containmentSimulationEngine.simulateContainmentByUsername("rahul.sharma");
        assertNotNull(simulation);
        assertEquals("rahul.sharma", simulation.username());
        assertTrue(simulation.disruptionScore() >= 50);
    }
}
