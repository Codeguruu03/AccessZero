package com.accesszero.service;

import com.accesszero.domain.entity.AuditEventEntity;
import com.accesszero.dto.AuditEventDto;
import com.accesszero.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AuditEventEntity recordEvent(Long operationId, String actor, String action, String target, String result, Object details) {
        String detailsJson = "{}";
        if (details != null) {
            try {
                if (details instanceof String str) {
                    detailsJson = str;
                } else {
                    detailsJson = objectMapper.writeValueAsString(details);
                }
            } catch (Exception e) {
                log.warn("Failed to serialize audit details to JSON: {}", e.getMessage());
                detailsJson = String.valueOf(details);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String checksum = calculateChecksum(operationId, actor, action, target, result, detailsJson, now);

        AuditEventEntity event = new AuditEventEntity(
                operationId,
                actor,
                action,
                target,
                result,
                detailsJson,
                checksum
        );
        event.setTimestamp(now);

        AuditEventEntity saved = auditEventRepository.save(event);
        log.info("IMMUTABLE AUDIT LOGGED [#{}] Action: {} | Actor: {} | Target: {} | Result: {} | Hash: {}",
                saved.getId(), action, actor, target, result, checksum);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> getAllEvents() {
        return auditEventRepository.findAllByOrderByTimestampDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> getEventsForOperation(Long operationId) {
        return auditEventRepository.findByOperationId(operationId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> getEventsForTarget(String target) {
        return auditEventRepository.findByTargetOrderByTimestampDesc(target).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> getEventsForActor(String actor) {
        return auditEventRepository.findByActor(actor).stream()
                .map(this::toDto)
                .toList();
    }

    public boolean verifyIntegrity(AuditEventEntity event) {
        if (event == null || event.getChecksum() == null) {
            return false;
        }
        String calculated = calculateChecksum(
                event.getOperationId(),
                event.getActor(),
                event.getAction(),
                event.getTarget(),
                event.getResult(),
                event.getDetailsJson(),
                event.getTimestamp()
        );
        return calculated.equalsIgnoreCase(event.getChecksum());
    }

    private String calculateChecksum(Long opId, String actor, String action, String target, String result, String detailsJson, LocalDateTime time) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = String.format("%s:%s:%s:%s:%s:%s:%s",
                    opId != null ? opId : "0",
                    actor != null ? actor : "",
                    action != null ? action : "",
                    target != null ? target : "",
                    result != null ? result : "",
                    detailsJson != null ? detailsJson : "",
                    time != null ? time.toString() : ""
            );
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm unavailable", e);
            return "UNKNOWN_HASH";
        }
    }

    public AuditEventDto toDto(AuditEventEntity entity) {
        return new AuditEventDto(
                entity.getId(),
                entity.getOperationId(),
                entity.getActor(),
                entity.getAction(),
                entity.getTarget(),
                entity.getResult(),
                entity.getDetailsJson(),
                entity.getTimestamp(),
                entity.getChecksum()
        );
    }
}
