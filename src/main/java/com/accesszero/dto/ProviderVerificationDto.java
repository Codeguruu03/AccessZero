package com.accesszero.dto;

import java.util.List;

public record ProviderVerificationDto(
        String providerName,
        String status, // CONTAINED, PARTIAL, FAILED
        String details,
        List<String> itemsChecked,
        List<String> remainingRisks
) {}
