package com.company.fulfillment.inventory.repository;

import com.company.fulfillment.inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WarehouseRepository
        extends JpaRepository<Warehouse, UUID> {

    Optional<Warehouse> findByTenantIdAndCode(
            UUID tenantId,
            String code
    );

    Optional<Warehouse> findByIdAndTenantId(
            UUID warehouseId,
            UUID tenantId
    );
}
