package com.accesszero.domain.entity;

import com.accesszero.domain.enums.PathType;
import jakarta.persistence.*;

@Entity
@Table(name = "access_paths")
public class AccessPathEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "path_description", nullable = false)
    private String pathDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "path_type", nullable = false)
    private PathType pathType;

    @Column(name = "is_privileged", nullable = false)
    private boolean isPrivileged = false;

    @Column(nullable = false)
    private boolean revoked = false;

    public AccessPathEntity() {}

    public AccessPathEntity(Long userId, Long applicationId, String pathDescription, PathType pathType, boolean isPrivileged, boolean revoked) {
        this.userId = userId;
        this.applicationId = applicationId;
        this.pathDescription = pathDescription;
        this.pathType = pathType;
        this.isPrivileged = isPrivileged;
        this.revoked = revoked;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getPathDescription() { return pathDescription; }
    public void setPathDescription(String pathDescription) { this.pathDescription = pathDescription; }

    public PathType getPathType() { return pathType; }
    public void setPathType(PathType pathType) { this.pathType = pathType; }

    public boolean isPrivileged() { return isPrivileged; }
    public void setPrivileged(boolean privileged) { isPrivileged = privileged; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
