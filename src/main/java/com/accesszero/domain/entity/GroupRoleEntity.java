package com.accesszero.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "group_roles")
public class GroupRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    public GroupRoleEntity() {}

    public GroupRoleEntity(Long groupId, Long roleId) {
        this.groupId = groupId;
        this.roleId = roleId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
