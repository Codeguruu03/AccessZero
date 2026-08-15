package com.accesszero.controller;

import com.accesszero.domain.entity.UserEntity;
import com.accesszero.dto.VerificationResultDto;
import com.accesszero.repository.UserRepository;
import com.accesszero.service.VerificationEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/verification")
public class VerificationController {

    private final VerificationEngine verificationEngine;
    private final UserRepository userRepository;

    public VerificationController(VerificationEngine verificationEngine, UserRepository userRepository) {
        this.verificationEngine = verificationEngine;
        this.userRepository = userRepository;
    }

    @PostMapping("/operation/{operationId}/user/{userId}")
    public ResponseEntity<VerificationResultDto> verifyByOperation(
            @PathVariable Long operationId,
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        String verifier = payload != null && payload.containsKey("verifiedBy") ? payload.get("verifiedBy") : "IT_ADMIN";
        VerificationResultDto result = verificationEngine.verifyContainment(operationId, userId, verifier);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<VerificationResultDto> verifyUserStatus(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "IT_ADMIN") String verifiedBy
    ) {
        VerificationResultDto result = verificationEngine.verifyContainment(null, userId, verifiedBy);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<VerificationResultDto> verifyUsernameStatus(
            @PathVariable String username,
            @RequestParam(defaultValue = "IT_ADMIN") String verifiedBy
    ) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        VerificationResultDto result = verificationEngine.verifyContainment(null, user.getId(), verifiedBy);
        return ResponseEntity.ok(result);
    }
}
