package com.accesszero.service;

import com.accesszero.domain.entity.UserEntity;
import com.accesszero.domain.enums.ContainmentStatus;
import com.accesszero.domain.enums.UserStatus;
import com.accesszero.dto.*;
import com.accesszero.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ContainmentEngineTest {

    @Autowired
    private ContainmentEngine containmentEngine;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testTwoPersonApprovalWorkflow() {
        Optional<UserEntity> userOpt = userRepository.findByUsername("rahul.sharma");
        assertTrue(userOpt.isPresent());
        UserEntity user = userOpt.get();

        // 1. Admin A requests containment (without emergency override)
        ContainmentRequestDto request = new ContainmentRequestDto(
                user.getId(),
                user.getUsername(),
                "anil.admin",
                "Testing Two-Person Rule",
                false
        );

        ContainmentResultDto pendingResult = containmentEngine.requestContainment(request);
        assertNotNull(pendingResult);
        assertTrue(pendingResult.requiresApproval(), "High-risk identity containment must require two-person approval");
        assertEquals(ContainmentStatus.CONTAINMENT_PENDING, pendingResult.status());

        // 2. Admin A attempts to approve own request -> MUST FAIL (Two-Person rule)
        ContainmentApprovalDto selfApproval = new ContainmentApprovalDto("anil.admin", "Self approval attempt");
        assertThrows(IllegalArgumentException.class, () -> {
            containmentEngine.approveContainment(pendingResult.operationId(), selfApproval);
        });

        // 3. Admin B approves containment -> SUCCESS
        ContainmentApprovalDto validApproval = new ContainmentApprovalDto("priya.security", "Approved by SecOps lead");
        ContainmentResultDto approvedResult = containmentEngine.approveContainment(pendingResult.operationId(), validApproval);

        assertNotNull(approvedResult);
        assertEquals("priya.security", approvedResult.approvedBy());
        assertTrue(approvedResult.status() == ContainmentStatus.CONTAINED || approvedResult.status() == ContainmentStatus.PARTIAL);
        assertFalse(approvedResult.actionsExecuted().isEmpty());

        // 4. Test Rollback / Restore
        RollbackResultDto rollbackResult = containmentEngine.rollbackContainment(approvedResult.operationId(), "anil.admin");
        assertNotNull(rollbackResult);
        assertEquals("RESTORED", rollbackResult.status());

        UserEntity restoredUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(UserStatus.ACTIVE, restoredUser.getStatus());
    }

    @Test
    void testEmergencyOverrideContainment() {
        Optional<UserEntity> userOpt = userRepository.findByUsername("rahul.sharma");
        assertTrue(userOpt.isPresent());
        UserEntity user = userOpt.get();

        ContainmentRequestDto emergencyRequest = new ContainmentRequestDto(
                user.getId(),
                user.getUsername(),
                "emergency.ciso",
                "DEFCON 1 Active Breach",
                true // Emergency override
        );

        ContainmentResultDto result = containmentEngine.requestContainment(emergencyRequest);
        assertNotNull(result);
        assertFalse(result.requiresApproval(), "Emergency override must bypass pending approval");
        assertTrue(result.status() == ContainmentStatus.CONTAINED || result.status() == ContainmentStatus.PARTIAL);
        assertTrue(result.accessPathsRevoked() > 0);

        // Cleanup: Rollback to active state
        containmentEngine.rollbackContainment(result.operationId(), "emergency.ciso");
    }
}
