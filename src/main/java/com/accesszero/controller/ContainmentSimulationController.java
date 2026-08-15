package com.accesszero.controller;

import com.accesszero.dto.ContainmentSimulationDto;
import com.accesszero.service.ContainmentSimulationEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/identities")
public class ContainmentSimulationController {

    private final ContainmentSimulationEngine containmentSimulationEngine;

    public ContainmentSimulationController(ContainmentSimulationEngine containmentSimulationEngine) {
        this.containmentSimulationEngine = containmentSimulationEngine;
    }

    @PostMapping("/{userId}/simulate")
    public ResponseEntity<ContainmentSimulationDto> simulateContainmentByUserId(@PathVariable Long userId) {
        ContainmentSimulationDto simulation = containmentSimulationEngine.simulateContainment(userId);
        return ResponseEntity.ok(simulation);
    }

    @PostMapping("/username/{username}/simulate")
    public ResponseEntity<ContainmentSimulationDto> simulateContainmentByUsername(@PathVariable String username) {
        ContainmentSimulationDto simulation = containmentSimulationEngine.simulateContainmentByUsername(username);
        return ResponseEntity.ok(simulation);
    }
}
