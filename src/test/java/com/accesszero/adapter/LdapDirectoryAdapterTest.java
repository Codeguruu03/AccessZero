package com.accesszero.adapter;

import com.accesszero.adapter.ldap.LdapDirectoryAdapter;
import com.accesszero.adapter.ldap.LdapGroupRepresentation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LdapDirectoryAdapterTest {

    @Autowired
    private LdapDirectoryAdapter ldapDirectoryAdapter;

    @Test
    void testGetUserGroupMemberships() {
        List<LdapGroupRepresentation> groups = ldapDirectoryAdapter.getUserGroupMemberships("rahul.sharma");
        assertNotNull(groups);
        assertTrue(groups.stream().anyMatch(g -> g.cn().equals("payroll-admin")));
        assertTrue(groups.stream().anyMatch(g -> g.cn().equals("finance")));
    }

    @Test
    void testContainLdapIdentity() {
        List<String> privileged = List.of("finance", "payroll-admin", "vpn-users");
        List<String> removed = ldapDirectoryAdapter.containLdapIdentity("rahul.sharma", privileged);
        assertEquals(3, removed.size());
        assertTrue(removed.contains("payroll-admin"));
    }
}
