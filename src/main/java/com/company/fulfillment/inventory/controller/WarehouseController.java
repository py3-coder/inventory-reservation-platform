package com.company.fulfillment.inventory.controller;

import com.company.fulfillment.config.TenantContext;
import com.company.fulfillment.inventory.dto.WarehouseRequest;
import com.company.fulfillment.inventory.dto.WarehouseResponse;
import com.company.fulfillment.inventory.service.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(
            WarehouseService warehouseService
    ) {
        this.warehouseService = warehouseService;
    }

    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody WarehouseRequest request
    ) {

        TenantContext.validateTenant(tenantId);

        WarehouseResponse response =
                warehouseService.createWarehouse(
                        tenantId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{warehouseId}")
    public ResponseEntity<WarehouseResponse> getWarehouse(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID warehouseId
    ) {

        TenantContext.validateTenant(tenantId);

        WarehouseResponse response =
                warehouseService.getWarehouse(
                        tenantId,
                        warehouseId
                );

        return ResponseEntity.ok(response);
    }
}

