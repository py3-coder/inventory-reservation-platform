package com.company.fulfillment.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductRequest(

        @NotBlank(message = "SKU is required")
        String sku,

        @NotBlank(message = "Product name is required")
        String name,

        String description

) {
}

