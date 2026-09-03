package com.company.fulfillment.events.dto;

import java.util.UUID;

public record OrderConfirmedEvent(
        UUID tenantId,
        UUID orderId
) {}