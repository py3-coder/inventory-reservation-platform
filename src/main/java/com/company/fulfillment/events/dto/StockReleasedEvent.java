package com.company.fulfillment.events.dto;

import java.util.UUID;

public record StockReleasedEvent(
        UUID tenantId,
        UUID reservationId
) {}