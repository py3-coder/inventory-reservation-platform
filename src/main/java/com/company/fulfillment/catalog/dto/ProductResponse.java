package com.company.fulfillment.catalog.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(

        UUID id,

        UUID tenantId,

        String sku,

        String name,

        String description,

        Instant createdAt,

        Instant updatedAt

) {
}
