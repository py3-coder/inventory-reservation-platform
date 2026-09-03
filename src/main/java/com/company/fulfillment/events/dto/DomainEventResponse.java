package com.company.fulfillment.events.dto;

import java.time.Instant;
import java.util.UUID;

public record DomainEventResponse(
        UUID id,
        UUID tenantId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        String payload,
        Instant createdAt
) {
}