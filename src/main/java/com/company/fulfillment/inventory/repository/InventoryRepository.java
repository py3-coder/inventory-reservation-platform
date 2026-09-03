package com.company.fulfillment.inventory.repository;

import com.company.fulfillment.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM Inventory i
        WHERE i.tenantId = :tenantId
          AND i.productId = :productId
        ORDER BY i.warehouseId
    """)
    List<Inventory> findForUpdate(
            @Param("tenantId") UUID tenantId,
            @Param("productId") UUID productId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT i
    FROM Inventory i
    WHERE i.tenantId = :tenantId
      AND i.productId = :productId
      AND i.warehouseId = :warehouseId
""")
    Optional<Inventory> findForUpdateByWarehouse(
            @Param("tenantId") UUID tenantId,
            @Param("productId") UUID productId,
            @Param("warehouseId") UUID warehouseId
    );

    Optional<Inventory> findByTenantIdAndProductIdAndWarehouseId(
            UUID tenantId,
            UUID productId,
            UUID warehouseId
    );
}