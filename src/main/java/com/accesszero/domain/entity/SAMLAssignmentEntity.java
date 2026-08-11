package com.accesszero.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saml_assignments")
public class SAMLAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "slo_supported")
    private boolean sloSupported = false;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt = LocalDateTime.now();

    public SAMLAssignmentEntity() {}

    public SAMLAssignmentEntity(Long userId, Long applicationId, String entityId, boolean sloSupported) {
        this.userId = userId;
        this.applicationId = applicationId;
        this.entityId = entityId;
        this.sloSupported = sloSupported;
        this.assignedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public boolean isSloSupported() { return sloSupported; }
    public void setSloSupported(boolean sloSupported) { this.sloSupported = sloSupported; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
}
