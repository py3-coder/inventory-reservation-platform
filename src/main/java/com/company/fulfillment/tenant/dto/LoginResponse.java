package com.company.fulfillment.tenant.dto;

public record LoginResponse(
        String accessToken,
        String tokenType
) {}