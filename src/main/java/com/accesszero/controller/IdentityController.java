package com.accesszero.controller;

import com.accesszero.domain.entity.UserEntity;
import com.accesszero.dto.BlastRadiusDto;
import com.accesszero.dto.IdentitySummaryDto;
import com.accesszero.repository.*;
import com.accesszero.service.BlastRadiusEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/identities")
public class IdentityController {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final OAuthTokenRepository oAuthTokenRepository;
    private final UserGroupRepository userGroupRepository;
    private final AccessPathRepository accessPathRepository;
    private final BlastRadiusEngine blastRadiusEngine;
    private final com.accesszero.service.IdentitySyncService identitySyncService;

    public IdentityController(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            OAuthTokenRepository oAuthTokenRepository,
            UserGroupRepository userGroupRepository,
            AccessPathRepository accessPathRepository,
            BlastRadiusEngine blastRadiusEngine,
            com.accesszero.service.IdentitySyncService identitySyncService
    ) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.oAuthTokenRepository = oAuthTokenRepository;
        this.userGroupRepository = userGroupRepository;
        this.accessPathRepository = accessPathRepository;
        this.blastRadiusEngine = blastRadiusEngine;
        this.identitySyncService = identitySyncService;
    }

    @PostMapping("/sync/{username}")
    public ResponseEntity<Map<String, Object>> syncIdentity(@PathVariable String username) {
        return ResponseEntity.ok(identitySyncService.syncIdentity(username));
    }

    @GetMapping
    public ResponseEntity<List<IdentitySummaryDto>> getAllIdentities() {
        List<UserEntity> users = userRepository.findAll();
        List<IdentitySummaryDto> summaries = new ArrayList<>();

        for (UserEntity user : users) {
            BlastRadiusDto blastRadius = blastRadiusEngine.calculateBlastRadius(user.getId());
            int sessions = userSessionRepository.findByUserIdAndActive(user.getId(), true).size();
            int tokens = oAuthTokenRepository.findByUserIdAndRevoked(user.getId(), false).size();
            int groups = userGroupRepository.findByUserId(user.getId()).size();
            int paths = accessPathRepository.findByUserId(user.getId()).size();

            summaries.add(new IdentitySummaryDto(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getDepartment(),
                    user.getStatus(),
                    sessions,
                    tokens,
                    groups,
                    blastRadius.applicationsAffectedCount(),
                    paths > 0 ? paths : blastRadius.totalAccessPathsCount(),
                    blastRadius.riskLevel(),
                    blastRadius.riskScore()
            ));
        }

        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserEntity> getIdentityById(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserEntity> getIdentityByUsername(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
