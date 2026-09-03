package com.company.fulfillment.inventory.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(

        UUID id,

        UUID tenantId,

        UUID productId,

        UUID warehouseId,

        int onHand,

        int reserved,

        int available,

        Instant createdAt,

        Instant updatedAt

) {
}

