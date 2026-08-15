package com.accesszero.simulation;

import com.accesszero.domain.entity.UserEntity;
import com.accesszero.dto.ContainmentSimulationDto;
import com.accesszero.repository.UserRepository;
import com.accesszero.service.ContainmentSimulationEngine;
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
        assertEquals(14, simulation.tokensToRevokeCount());
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
