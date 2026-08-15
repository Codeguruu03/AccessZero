package com.accesszero.service;

import com.accesszero.domain.entity.AuditEventEntity;
import com.accesszero.dto.AuditEventDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuditServiceTest {

    @Autowired
    private AuditService auditService;

    @Test
    void testRecordAndVerifyImmutableAuditEvent() {
        AuditEventEntity event = auditService.recordEvent(
                100L,
                "test.admin",
                "TEST_ACTION",
                "target.user",
                "SUCCESS",
                Map.of("field", "value", "count", 42)
        );

        assertNotNull(event);
        assertNotNull(event.getId());
        assertNotNull(event.getChecksum());
        assertEquals("test.admin", event.getActor());
        assertEquals("TEST_ACTION", event.getAction());
        assertEquals("target.user", event.getTarget());
        assertEquals("SUCCESS", event.getResult());

        // Validate cryptographic integrity
        boolean isValid = auditService.verifyIntegrity(event);
        assertTrue(isValid, "SHA-256 Checksum must match calculated hash");

        // Query by target
        List<AuditEventDto> targetEvents = auditService.getEventsForTarget("target.user");
        assertFalse(targetEvents.isEmpty());
        assertTrue(targetEvents.stream().anyMatch(e -> e.id().equals(event.getId())));
    }
}
