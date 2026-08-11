package com.accesszero.domain.entity;

import com.accesszero.domain.enums.GroupType;
import jakarta.persistence.*;

@Entity
@Table(name = "groups")
public class GroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(name = "is_privileged", nullable = false)
    private boolean isPrivileged = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupType type = GroupType.INTERNAL;

    public GroupEntity() {}

    public GroupEntity(String name, String description, boolean isPrivileged, GroupType type) {
        this.name = name;
        this.description = description;
        this.isPrivileged = isPrivileged;
        this.type = type;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPrivileged() { return isPrivileged; }
    public void setPrivileged(boolean privileged) { isPrivileged = privileged; }

    public GroupType getType() { return type; }
    public void setType(GroupType type) { this.type = type; }
}
