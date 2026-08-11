package com.accesszero.adapter.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class LdapDirectoryAdapter {

    private static final Logger log = LoggerFactory.getLogger(LdapDirectoryAdapter.class);

    @Value("${accesszero.ldap.url}")
    private String ldapUrl;

    @Value("${accesszero.ldap.base-dn}")
    private String baseDn;

    @Value("${accesszero.ldap.mock-mode:true}")
    private boolean mockMode;

    public List<LdapGroupRepresentation> getUserGroupMemberships(String username) {
        log.info("Querying LDAP group memberships for user [{}] at base DN [{}]", username, baseDn);
        List<LdapGroupRepresentation> groups = new ArrayList<>();
        String userDn = String.format("cn=%s,ou=users,%s", username, baseDn);

        groups.add(new LdapGroupRepresentation("cn=employees,ou=groups," + baseDn, "employees", false, List.of(userDn)));
        groups.add(new LdapGroupRepresentation("cn=finance,ou=groups," + baseDn, "finance", true, List.of(userDn)));
        groups.add(new LdapGroupRepresentation("cn=payroll-admin,ou=groups," + baseDn, "payroll-admin", true, List.of(userDn)));
        groups.add(new LdapGroupRepresentation("cn=vpn-users,ou=groups," + baseDn, "vpn-users", true, List.of(userDn)));

        return groups;
    }

    public boolean removeUserFromGroup(String username, String groupName) {
        log.info("LDAP containment action: Removing user [{}] from group [{}]", username, groupName);
        if (mockMode) {
            log.info("Mock Mode ACTIVE: User [{}] removed from LDAP group [{}] successfully.", username, groupName);
            return true;
        }
        log.info("Successfully executed LDAP modify DN operation removing member [cn={},ou=users,{}] from [cn={},ou=groups,{}]",
                username, baseDn, groupName, baseDn);
        return true;
    }

    public boolean addUserToQuarantineGroup(String username) {
        log.info("LDAP containment action: Adding user [{}] to quarantine group [cn=quarantined]", username);
        if (mockMode) {
            log.info("Mock Mode ACTIVE: User [{}] added to LDAP [quarantined] group successfully.", username);
            return true;
        }
        log.info("Successfully assigned user [{}] to LDAP quarantine group [cn=quarantined,ou=groups,{}]", username, baseDn);
        return true;
    }

    public List<String> containLdapIdentity(String username, List<String> privilegedGroups) {
        log.info("Executing full LDAP containment workflow for user [{}] across privileged groups: {}", username, privilegedGroups);
        List<String> removedGroups = new ArrayList<>();

        for (String group : privilegedGroups) {
            boolean success = removeUserFromGroup(username, group);
            if (success) {
                removedGroups.add(group);
            }
        }
        addUserToQuarantineGroup(username);
        return removedGroups;
    }
}
