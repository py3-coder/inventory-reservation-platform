package com.company.fulfillment.payment.event;

import java.util.UUID;

public record PaymentInitiatedEvent(
        UUID paymentId,
        UUID orderId,
        UUID tenantId
) {
}