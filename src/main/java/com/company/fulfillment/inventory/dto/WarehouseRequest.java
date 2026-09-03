package com.company.fulfillment.inventory.dto;

import jakarta.validation.constraints.NotBlank;

public record WarehouseRequest(

        @NotBlank(message = "Warehouse code is required")
        String code,

        @NotBlank(message = "Warehouse name is required")
        String name

) {

}

