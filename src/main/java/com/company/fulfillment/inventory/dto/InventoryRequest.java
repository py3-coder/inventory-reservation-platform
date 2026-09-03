package com.company.fulfillment.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;
public record InventoryRequest(

        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "Warehouse ID is required")
        UUID warehouseId,

        @Min(value = 0, message = "On-hand quantity cannot be negative")
        int onHand

) {
}

