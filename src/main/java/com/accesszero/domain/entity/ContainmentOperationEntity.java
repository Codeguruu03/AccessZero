package com.accesszero.domain.entity;

import com.accesszero.domain.enums.ContainmentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "containment_operations")
public class ContainmentOperationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContainmentStatus status = ContainmentStatus.NORMAL;

    @Column(name = "reason")
    private String reason;

    @Column(name = "access_paths_found")
    private Integer accessPathsFound = 0;

    @Column(name = "access_paths_revoked")
    private Integer accessPathsRevoked = 0;

    @Column(name = "requires_manual_action")
    private Integer requiresManualAction = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ContainmentOperationEntity() {}

    public ContainmentOperationEntity(Long targetUserId, String requestedBy, ContainmentStatus status, String reason) {
        this.targetUserId = targetUserId;
        this.requestedBy = requestedBy;
        this.status = status;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public ContainmentStatus getStatus() { return status; }
    public void setStatus(ContainmentStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getAccessPathsFound() { return accessPathsFound; }
    public void setAccessPathsFound(Integer accessPathsFound) { this.accessPathsFound = accessPathsFound; }

    public Integer getAccessPathsRevoked() { return accessPathsRevoked; }
    public void setAccessPathsRevoked(Integer accessPathsRevoked) { this.accessPathsRevoked = accessPathsRevoked; }

    public Integer getRequiresManualAction() { return requiresManualAction; }
    public void setRequiresManualAction(Integer requiresManualAction) { this.requiresManualAction = requiresManualAction; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
