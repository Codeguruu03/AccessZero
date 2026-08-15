package com.accesszero.dto;

import java.util.List;

public record ContainmentSimulationDto(
        Long userId,
        String username,
        String accountStatusCurrent,
        String accountStatusSimulated,
        int tokensToRevokeCount,
        int sessionsToTerminateCount,
        int ldapGroupsToRemoveCount,
        List<String> privilegedLdapGroupsToRemove,
        int applicationsAffectedCount,
        String disruptionLevel,
        int disruptionScore,
        boolean requiresApproval,
        List<String> simulatedActionSummary
) {}
