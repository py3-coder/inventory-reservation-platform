package com.company.fulfillment.inventory.controller;

import com.company.fulfillment.config.TenantContext;
import com.company.fulfillment.inventory.dto.InventoryRequest;
import com.company.fulfillment.inventory.dto.InventoryResponse;
import com.company.fulfillment.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(
            InventoryService inventoryService
    ) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> createOrAdjustInventory(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody InventoryRequest request) {

        TenantContext.validateTenant(tenantId);
        InventoryResponse response =
                inventoryService.createOrAdjustInventory(
                        tenantId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{productId}/{warehouseId}")
    public ResponseEntity<InventoryResponse> getInventory(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID productId,
            @PathVariable UUID warehouseId
    ) {
        TenantContext.validateTenant(tenantId);
        InventoryResponse response = inventoryService.getInventory(
                        tenantId,
                        productId,
                        warehouseId
                );

        return ResponseEntity.ok(response);
    }
}

