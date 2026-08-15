package com.accesszero.controller;

import com.accesszero.dto.BlastRadiusDto;
import com.accesszero.service.BlastRadiusEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/identities")
public class BlastRadiusController {

    private final BlastRadiusEngine blastRadiusEngine;

    public BlastRadiusController(BlastRadiusEngine blastRadiusEngine) {
        this.blastRadiusEngine = blastRadiusEngine;
    }

    @GetMapping("/{userId}/blast-radius")
    public ResponseEntity<BlastRadiusDto> getBlastRadiusByUserId(@PathVariable Long userId) {
        BlastRadiusDto blastRadius = blastRadiusEngine.calculateBlastRadius(userId);
        return ResponseEntity.ok(blastRadius);
    }

    @GetMapping("/username/{username}/blast-radius")
    public ResponseEntity<BlastRadiusDto> getBlastRadiusByUsername(@PathVariable String username) {
        BlastRadiusDto blastRadius = blastRadiusEngine.calculateBlastRadiusByUsername(username);
        return ResponseEntity.ok(blastRadius);
    }
}
