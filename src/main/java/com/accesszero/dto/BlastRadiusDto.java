package com.accesszero.dto;

import java.util.List;

public record BlastRadiusDto(
        Long userId,
        String username,
        String riskLevel,
        int riskScore,
        int activeSessionsCount,
        int oauthTokensCount,
        int ldapGroupsCount,
        int sensitiveGroupsCount,
        int applicationsAffectedCount,
        int privilegedApplicationsCount,
        int samlAssignmentsCount,
        int totalAccessPathsCount,
        int privilegedAccessPathsCount,
        List<AffectedApplicationDto> affectedApplications
) {}
