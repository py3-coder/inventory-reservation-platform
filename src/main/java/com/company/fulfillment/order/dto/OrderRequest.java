package com.company.fulfillment.order.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderRequest(
        @NotNull UUID reservationId
) {
}