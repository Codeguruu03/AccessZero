package com.accesszero.controller;

import com.accesszero.service.IdentitySyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sync")
public class IdentitySyncController {

    private final IdentitySyncService identitySyncService;

    public IdentitySyncController(IdentitySyncService identitySyncService) {
        this.identitySyncService = identitySyncService;
    }

    @PostMapping("/identity")
    public ResponseEntity<Map<String, Object>> syncIdentity(@RequestParam String username) {
        Map<String, Object> result = identitySyncService.syncIdentity(username);
        return ResponseEntity.ok(result);
    }
}
