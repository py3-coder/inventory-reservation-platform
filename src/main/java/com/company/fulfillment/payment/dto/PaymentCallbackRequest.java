package com.company.fulfillment.payment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentCallbackRequest(
        @NotNull UUID paymentId,
        @NotNull UUID orderId,
        @NotNull String status
) {}