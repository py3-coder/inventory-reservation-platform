package com.company.fulfillment.inventory.service;

import com.company.fulfillment.events.service.DomainEventService;
import com.company.fulfillment.inventory.entity.Inventory;
import com.company.fulfillment.inventory.entity.Reservation;
import com.company.fulfillment.inventory.entity.ReservationAllocation;
import com.company.fulfillment.inventory.entity.ReservationItem;
import com.company.fulfillment.inventory.repository.InventoryRepository;
import com.company.fulfillment.inventory.repository.ReservationAllocationRepository;
import com.company.fulfillment.inventory.repository.ReservationItemRepository;
import com.company.fulfillment.inventory.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationExpiryService {

    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final ReservationAllocationRepository allocationRepository;
    private final InventoryRepository inventoryRepository;
    private final DomainEventService domainEventService;

    public ReservationExpiryService(
            ReservationRepository reservationRepository,
            ReservationItemRepository reservationItemRepository,
            ReservationAllocationRepository allocationRepository,
            InventoryRepository inventoryRepository,
            DomainEventService domainEventService
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationItemRepository = reservationItemRepository;
        this.allocationRepository = allocationRepository;
        this.inventoryRepository = inventoryRepository;
        this.domainEventService = domainEventService;
    }
    @Transactional
    public void expireReservation(
            UUID reservationId,
            UUID tenantId
    ) {
        Reservation reservation =
                reservationRepository
                        .findForUpdate(reservationId, tenantId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Reservation not found"
                                )
                        );
        if (!"RESERVED".equals(reservation.getStatus())) {
            return;
        }
        if (reservation.getExpiresAt().isAfter(Instant.now())) {
            return;
        }

        // Release the reserved inventory
        releaseReservedStock(reservation);

        // Mark reservation expired
        reservation.setStatus("EXPIRED");

        // Persist domain event
        domainEventService.saveEvent(
                tenantId,
                "ReservationExpired",
                reservation.getId()
        );

        domainEventService.saveEvent(
                tenantId,
                "StockReleased",
                reservation.getId()
        );
    }

    private void releaseReservedStock(
            Reservation reservation
    ) {

        List<ReservationItem> items =
                reservationItemRepository
                        .findByReservationId(
                                reservation.getId()
                        );

        for (ReservationItem item : items) {

            List<ReservationAllocation> allocations =
                    allocationRepository
                            .findByReservationItemId(
                                    item.getId()
                            );

            for (ReservationAllocation allocation : allocations) {
                Inventory inventory =
                        inventoryRepository
                                .findForUpdateByWarehouse(
                                        reservation.getTenantId(),
                                        item.getProductId(),
                                        allocation.getWarehouseId()
                                )
                                .orElseThrow(() ->
                                        new RuntimeException(
                                                "Inventory not found"
                                        )
                                );

                int quantity = allocation.getQuantity();

                if (inventory.getReserved() < quantity) {
                    throw new RuntimeException(
                            "Reserved stock cannot become negative"
                    );
                }

                inventory.setReserved(
                        inventory.getReserved() - quantity
                );
            }
        }
    }
}