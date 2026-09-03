package com.company.fulfillment.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReservationRequest(

        @NotBlank
        String sku,

        @Min(1)
        int quantity
) {
}