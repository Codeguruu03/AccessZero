package com.accesszero.controller;

import com.accesszero.dto.AuditEventDto;
import com.accesszero.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/events")
    public ResponseEntity<List<AuditEventDto>> getAllAuditEvents() {
        return ResponseEntity.ok(auditService.getAllEvents());
    }

    @GetMapping("/operations/{operationId}")
    public ResponseEntity<List<AuditEventDto>> getEventsByOperation(@PathVariable Long operationId) {
        return ResponseEntity.ok(auditService.getEventsForOperation(operationId));
    }

    @GetMapping("/targets/{target}")
    public ResponseEntity<List<AuditEventDto>> getEventsByTarget(@PathVariable String target) {
        return ResponseEntity.ok(auditService.getEventsForTarget(target));
    }

    @GetMapping("/actors/{actor}")
    public ResponseEntity<List<AuditEventDto>> getEventsByActor(@PathVariable String actor) {
        return ResponseEntity.ok(auditService.getEventsForActor(actor));
    }
}
