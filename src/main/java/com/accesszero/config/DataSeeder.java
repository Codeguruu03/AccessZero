package com.accesszero.config;

import com.accesszero.domain.entity.*;
import com.accesszero.domain.enums.*;
import com.accesszero.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final ApplicationRepository applicationRepository;
    private final PermissionRepository permissionRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupRoleRepository groupRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserSessionRepository userSessionRepository;
    private final OAuthTokenRepository oAuthTokenRepository;
    private final SAMLAssignmentRepository samlAssignmentRepository;
    private final AccessPathRepository accessPathRepository;

    public DataSeeder(
            UserRepository userRepository,
            GroupRepository groupRepository,
            RoleRepository roleRepository,
            ApplicationRepository applicationRepository,
            PermissionRepository permissionRepository,
            UserGroupRepository userGroupRepository,
            GroupRoleRepository groupRoleRepository,
            RolePermissionRepository rolePermissionRepository,
            UserSessionRepository userSessionRepository,
            OAuthTokenRepository oAuthTokenRepository,
            SAMLAssignmentRepository samlAssignmentRepository,
            AccessPathRepository accessPathRepository
    ) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.roleRepository = roleRepository;
        this.applicationRepository = applicationRepository;
        this.permissionRepository = permissionRepository;
        this.userGroupRepository = userGroupRepository;
        this.groupRoleRepository = groupRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userSessionRepository = userSessionRepository;
        this.oAuthTokenRepository = oAuthTokenRepository;
        this.samlAssignmentRepository = samlAssignmentRepository;
        this.accessPathRepository = accessPathRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        // 1. Create Identity (Rahul Sharma)
        UserEntity rahul = userRepository.save(new UserEntity(
                "rahul.sharma",
                "rahul.sharma@company.com",
                "Rahul",
                "Sharma",
                "Finance",
                UserStatus.ACTIVE
        ));

        UserEntity admin = userRepository.save(new UserEntity(
                "anil.admin",
                "anil.admin@company.com",
                "Anil",
                "Admin",
                "IT Security",
                UserStatus.ACTIVE
        ));

        // 2. Create Groups
        GroupEntity empGroup = groupRepository.save(new GroupEntity("employees", "All Employees Group", false, GroupType.LDAP));
        GroupEntity finGroup = groupRepository.save(new GroupEntity("finance", "Finance Department Group", true, GroupType.LDAP));
        GroupEntity payrollGroup = groupRepository.save(new GroupEntity("payroll-admin", "Payroll Administrator Group", true, GroupType.KEYCLOAK));
        GroupEntity reportGroup = groupRepository.save(new GroupEntity("reporting", "Financial Reporting Group", false, GroupType.INTERNAL));
        GroupEntity vpnGroup = groupRepository.save(new GroupEntity("vpn-users", "Remote Access VPN Users", true, GroupType.LDAP));
        GroupEntity engGroup = groupRepository.save(new GroupEntity("engineering", "Engineering Team Group", false, GroupType.LDAP));

        // Assign Rahul to Groups
        userGroupRepository.save(new UserGroupEntity(rahul.getId(), empGroup.getId()));
        userGroupRepository.save(new UserGroupEntity(rahul.getId(), finGroup.getId()));
        userGroupRepository.save(new UserGroupEntity(rahul.getId(), payrollGroup.getId()));
        userGroupRepository.save(new UserGroupEntity(rahul.getId(), reportGroup.getId()));
        userGroupRepository.save(new UserGroupEntity(rahul.getId(), vpnGroup.getId()));

        // 3. Create Applications
        ApplicationEntity gitlab = applicationRepository.save(new ApplicationEntity("GitLab", "Source Code Management", ApplicationType.OIDC, SensitivityLevel.HIGH, true));
        ApplicationEntity jira = applicationRepository.save(new ApplicationEntity("Jira", "Issue Tracking", ApplicationType.OIDC, SensitivityLevel.MEDIUM, true));
        ApplicationEntity payrollApp = applicationRepository.save(new ApplicationEntity("Payroll System", "Core Financial Payroll App", ApplicationType.INTERNAL, SensitivityLevel.CRITICAL, true));
        ApplicationEntity vpnApp = applicationRepository.save(new ApplicationEntity("Corporate VPN", "Network Gateway", ApplicationType.INTERNAL, SensitivityLevel.CRITICAL, true));
        ApplicationEntity reportApp = applicationRepository.save(new ApplicationEntity("Internal Reporting", "BI and Analytics", ApplicationType.OIDC, SensitivityLevel.MEDIUM, true));
        ApplicationEntity workdaysaml = applicationRepository.save(new ApplicationEntity("Workday SAML", "HR Portal", ApplicationType.SAML, SensitivityLevel.HIGH, false));
        ApplicationEntity salesforcesaml = applicationRepository.save(new ApplicationEntity("Salesforce SAML", "CRM System", ApplicationType.SAML, SensitivityLevel.HIGH, true));

        // 4. Roles & Permissions
        RoleEntity payrollAdminRole = roleRepository.save(new RoleEntity("PayrollAdminRole", "Full access to Payroll disbursement"));
        RoleEntity finAnalystRole = roleRepository.save(new RoleEntity("FinanceAnalystRole", "Access to financial statements"));

        groupRoleRepository.save(new GroupRoleEntity(payrollGroup.getId(), payrollAdminRole.getId()));
        groupRoleRepository.save(new GroupRoleEntity(finGroup.getId(), finAnalystRole.getId()));

        PermissionEntity disbursePerm = permissionRepository.save(new PermissionEntity("DISBURSE_PAYROLL", "payroll-service", "EXECUTE"));
        PermissionEntity viewFinPerm = permissionRepository.save(new PermissionEntity("VIEW_FINANCIAL_REPORTS", "reporting-service", "READ"));

        rolePermissionRepository.save(new RolePermissionEntity(payrollAdminRole.getId(), disbursePerm.getId()));
        rolePermissionRepository.save(new RolePermissionEntity(finAnalystRole.getId(), viewFinPerm.getId()));

        // 5. Active User Sessions (7 sessions)
        for (int i = 1; i <= 7; i++) {
            userSessionRepository.save(new UserSessionEntity(
                    rahul.getId(),
                    "sess_keycloak_" + i,
                    i % 2 == 0 ? "Keycloak-OIDC" : "Direct-App",
                    "192.168.1." + (10 + i),
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                    true,
                    LocalDateTime.now().plusHours(8)
            ));
        }

        // 6. OAuth Refresh Tokens (14 tokens)
        for (int i = 1; i <= 14; i++) {
            oAuthTokenRepository.save(new OAuthTokenEntity(
                    rahul.getId(),
                    i % 2 == 0 ? TokenType.REFRESH : TokenType.ACCESS,
                    "client_app_" + (i % 4 + 1),
                    "hash_token_val_" + i,
                    "openid profile email finance.write",
                    false,
                    LocalDateTime.now().plusDays(30)
            ));
        }

        // 7. SAML Assignments
        samlAssignmentRepository.save(new SAMLAssignmentEntity(rahul.getId(), workdaysaml.getId(), "https://workday.com/saml/metadata", false));
        samlAssignmentRepository.save(new SAMLAssignmentEntity(rahul.getId(), salesforcesaml.getId(), "https://salesforce.com/saml/metadata", true));

        // 8. Access Paths seed
        accessPathRepository.save(new AccessPathEntity(rahul.getId(), payrollApp.getId(), "Rahul -> payroll-admin -> PayrollAdminRole -> Payroll System", PathType.GROUP_INHERITED, true, false));
        accessPathRepository.save(new AccessPathEntity(rahul.getId(), vpnApp.getId(), "Rahul -> vpn-users -> Corporate VPN", PathType.GROUP_INHERITED, true, false));
        accessPathRepository.save(new AccessPathEntity(rahul.getId(), gitlab.getId(), "Rahul -> OAuth Refresh Token -> GitLab", PathType.TOKEN_BASED, false, false));
        accessPathRepository.save(new AccessPathEntity(rahul.getId(), jira.getId(), "Rahul -> Active Session -> Jira", PathType.TOKEN_BASED, false, false));
        accessPathRepository.save(new AccessPathEntity(rahul.getId(), workdaysaml.getId(), "Rahul -> SAML Assignment -> Workday SAML", PathType.SAML_ASSIGNED, true, false));
        accessPathRepository.save(new AccessPathEntity(rahul.getId(), salesforcesaml.getId(), "Rahul -> SAML Assignment -> Salesforce SAML", PathType.SAML_ASSIGNED, false, false));
    }
}
