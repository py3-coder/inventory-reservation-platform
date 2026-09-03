package com.company.fulfillment.inventory.service;

import com.company.fulfillment.inventory.dto.WarehouseRequest;
import com.company.fulfillment.inventory.dto.WarehouseResponse;
import com.company.fulfillment.inventory.entity.Warehouse;
import com.company.fulfillment.inventory.repository.WarehouseRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(
            WarehouseRepository warehouseRepository
    ) {
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public WarehouseResponse createWarehouse(
            UUID tenantId,
            WarehouseRequest request
    ) {

        if (warehouseRepository
                .findByTenantIdAndCode(
                        tenantId,
                        request.code()
                )
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Warehouse with code already exists"
            );
        }

        Warehouse warehouse = new Warehouse();

        warehouse.setTenantId(tenantId);
        warehouse.setCode(request.code());
        warehouse.setName(request.name());

        try {

            warehouse = warehouseRepository.save(warehouse);

        } catch (DataIntegrityViolationException exception) {

            // Database unique constraint is the final protection
            // against concurrent duplicate warehouse creation.
            throw new IllegalArgumentException(
                    "Warehouse with code already exists",
                    exception
            );
        }

        return toResponse(warehouse);
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouse(
            UUID tenantId,
            UUID warehouseId
    ) {

        Warehouse warehouse =
                warehouseRepository
                        .findByIdAndTenantId(warehouseId, tenantId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Warehouse not found"
                                )
                        );
        return toResponse(warehouse);
    }

    private WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getTenantId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getCreatedAt()
        );
    }
}
