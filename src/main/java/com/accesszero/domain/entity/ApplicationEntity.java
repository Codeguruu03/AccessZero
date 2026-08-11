package com.accesszero.domain.entity;

import com.accesszero.domain.enums.ApplicationType;
import com.accesszero.domain.enums.SensitivityLevel;
import jakarta.persistence.*;

@Entity
@Table(name = "applications")
public class ApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationType type = ApplicationType.INTERNAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensitivity_level", nullable = false)
    private SensitivityLevel sensitivityLevel = SensitivityLevel.MEDIUM;

    @Column(name = "supports_remote_logout")
    private boolean supportsRemoteLogout = true;

    public ApplicationEntity() {}

    public ApplicationEntity(String name, String description, ApplicationType type, SensitivityLevel sensitivityLevel, boolean supportsRemoteLogout) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.sensitivityLevel = sensitivityLevel;
        this.supportsRemoteLogout = supportsRemoteLogout;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ApplicationType getType() { return type; }
    public void setType(ApplicationType type) { this.type = type; }

    public SensitivityLevel getSensitivityLevel() { return sensitivityLevel; }
    public void setSensitivityLevel(SensitivityLevel sensitivityLevel) { this.sensitivityLevel = sensitivityLevel; }

    public boolean isSupportsRemoteLogout() { return supportsRemoteLogout; }
    public void setSupportsRemoteLogout(boolean supportsRemoteLogout) { this.supportsRemoteLogout = supportsRemoteLogout; }
}
