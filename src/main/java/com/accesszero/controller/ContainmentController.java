package com.accesszero.controller;

import com.accesszero.domain.entity.ContainmentOperationEntity;
import com.accesszero.dto.ContainmentApprovalDto;
import com.accesszero.dto.ContainmentRequestDto;
import com.accesszero.dto.ContainmentResultDto;
import com.accesszero.dto.RollbackResultDto;
import com.accesszero.service.ContainmentEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/containment")
public class ContainmentController {

    private final ContainmentEngine containmentEngine;

    public ContainmentController(ContainmentEngine containmentEngine) {
        this.containmentEngine = containmentEngine;
    }

    @PostMapping("/request")
    public ResponseEntity<ContainmentResultDto> requestContainment(@RequestBody ContainmentRequestDto request) {
        ContainmentResultDto result = containmentEngine.requestContainment(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{operationId}/approve")
    public ResponseEntity<ContainmentResultDto> approveContainment(
            @PathVariable Long operationId,
            @RequestBody ContainmentApprovalDto approval
    ) {
        ContainmentResultDto result = containmentEngine.approveContainment(operationId, approval);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{operationId}/reject")
    public ResponseEntity<ContainmentResultDto> rejectContainment(
            @PathVariable Long operationId,
            @RequestBody ContainmentApprovalDto rejection
    ) {
        ContainmentResultDto result = containmentEngine.rejectContainment(operationId, rejection);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{operationId}/rollback")
    public ResponseEntity<RollbackResultDto> rollbackContainment(
            @PathVariable Long operationId,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        String admin = payload != null && payload.containsKey("rolledBackBy") ? payload.get("rolledBackBy") : "it.admin";
        RollbackResultDto result = containmentEngine.rollbackContainment(operationId, admin);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/operations")
    public ResponseEntity<List<ContainmentOperationEntity>> getAllOperations() {
        return ResponseEntity.ok(containmentEngine.getAllOperations());
    }

    @GetMapping("/operations/{operationId}")
    public ResponseEntity<ContainmentOperationEntity> getOperationById(@PathVariable Long operationId) {
        return containmentEngine.getOperation(operationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
