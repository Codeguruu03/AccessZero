package com.accesszero.adapter.ldap;

import java.util.List;

public record LdapGroupRepresentation(
        String dn,
        String cn,
        boolean isPrivileged,
        List<String> members
) {}
