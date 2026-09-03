package com.company.fulfillment.inventory.service;

import com.company.fulfillment.catalog.entity.Product;
import com.company.fulfillment.catalog.repository.ProductRepository;
import com.company.fulfillment.inventory.dto.InventoryRequest;
import com.company.fulfillment.inventory.dto.InventoryResponse;
import com.company.fulfillment.inventory.entity.Inventory;
import com.company.fulfillment.inventory.entity.Warehouse;
import com.company.fulfillment.inventory.repository.InventoryRepository;
import com.company.fulfillment.inventory.repository.WarehouseRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    public InventoryService(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public InventoryResponse createOrAdjustInventory(
            UUID tenantId,
            InventoryRequest request
    ) {

        // Verify product belongs to this tenant.
        Product product = productRepository
                        .findByIdAndTenantId(
                                request.productId(),
                                tenantId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Product not found"
                                )
                        );

        // Verify warehouse belongs to this tenant.
        Warehouse warehouse = warehouseRepository
                        .findByIdAndTenantId(
                                request.warehouseId(),
                                tenantId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Warehouse not found"
                                )
                        );

        Inventory inventory = inventoryRepository
                        .findForUpdateByWarehouse(
                                tenantId,
                                product.getId(),
                                warehouse.getId()
                        )
                        .orElse(null);

        if (inventory == null) {
            inventory = new Inventory();
            inventory.setTenantId(tenantId);
            inventory.setProductId(product.getId());
            inventory.setWarehouseId(warehouse.getId());
            inventory.setOnHand(request.onHand());
            inventory.setReserved(0);

            try {
                inventory = inventoryRepository.saveAndFlush(inventory);
            } catch (DataIntegrityViolationException exception) {

                inventory = inventoryRepository
                                .findForUpdateByWarehouse(
                                        tenantId,
                                        product.getId(),
                                        warehouse.getId()
                                )
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "Inventory could not be created"
                                        )
                                );

                if (request.onHand() < inventory.getReserved()) {
                    throw new IllegalArgumentException(
                            "On-hand quantity cannot be less than reserved quantity"
                    );
                }

                inventory.setOnHand(request.onHand());
            }

        } else {
            if (request.onHand() < inventory.getReserved()) {
                throw new IllegalArgumentException(
                        "On-hand quantity cannot be less than reserved quantity"
                );
            }
            inventory.setOnHand(request.onHand());
        }
        inventory = inventoryRepository.save(inventory);
        return toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(
            UUID tenantId,
            UUID productId,
            UUID warehouseId
    ) {
        Inventory inventory = inventoryRepository
                .findByTenantIdAndProductIdAndWarehouseId(
                        tenantId,
                        productId,
                        warehouseId
                )
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));
        return toResponse(inventory);
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getTenantId(),
                inventory.getProductId(),
                inventory.getWarehouseId(),
                inventory.getOnHand(),
                inventory.getReserved(),
                inventory.getAvailable(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}

