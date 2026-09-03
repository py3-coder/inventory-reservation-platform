package com.company.fulfillment.inventory.service;

import com.company.fulfillment.catalog.entity.Product;
import com.company.fulfillment.catalog.repository.ProductRepository;
import com.company.fulfillment.idempotency.entity.IdempotencyRecord;
import com.company.fulfillment.idempotency.repository.IdempotencyRecordRepository;
import com.company.fulfillment.inventory.dto.ReservationRequest;
import com.company.fulfillment.inventory.dto.ReservationResponse;
import com.company.fulfillment.inventory.entity.Inventory;
import com.company.fulfillment.inventory.entity.Reservation;
import com.company.fulfillment.inventory.entity.ReservationAllocation;
import com.company.fulfillment.inventory.entity.ReservationItem;
import com.company.fulfillment.inventory.repository.InventoryRepository;
import com.company.fulfillment.inventory.repository.ReservationAllocationRepository;
import com.company.fulfillment.inventory.repository.ReservationItemRepository;
import com.company.fulfillment.inventory.repository.ReservationRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class ReservationService {

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final ReservationAllocationRepository reservationAllocationRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    @Value("${app.reservation.ttl-seconds}")
    private long reservationTtlSeconds;

    public ReservationService(
            InventoryRepository inventoryRepository,
            ReservationRepository reservationRepository,
            ProductRepository productRepository,
            ReservationItemRepository reservationItemRepository,
            ReservationAllocationRepository reservationAllocationRepository,
            IdempotencyRecordRepository idempotencyRecordRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.productRepository = productRepository;
        this.reservationItemRepository = reservationItemRepository;
        this.reservationAllocationRepository = reservationAllocationRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    @Transactional
    public ReservationResponse reserve(UUID tenantId, UUID userId, String idempotencyKey , ReservationRequest request) {


        Optional<IdempotencyRecord> existing = idempotencyRecordRepository
                        .findByTenantIdAndUserIdAndIdempotencyKey(tenantId, userId, idempotencyKey);
        String requestHash = DigestUtils.sha256Hex(request.sku() + ":" + request.quantity());
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new RuntimeException(
                        "Idempotency key reused with different request"
                );
            }
            Reservation reservation = reservationRepository
                    .findById(existing.get().getReservationId())
                            .orElseThrow();

            return new ReservationResponse(reservation.getId(), reservation.getStatus(), reservation.getExpiresAt());
        }

        Product product = productRepository
                .findByTenantIdAndSku(tenantId, request.sku())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        List<Inventory> inventories = inventoryRepository.findForUpdate(tenantId, product.getId());

        int available = inventories.stream()
                .mapToInt(Inventory::getAvailable)
                .sum();

        if (available < request.quantity()) {
            throw new RuntimeException("Insufficient stock");
        }

        Reservation reservation = new Reservation();
        reservation.setTenantId(tenantId);
        reservation.setUserId(userId);
        reservation.setStatus("RESERVED");
        reservation.setExpiresAt(Instant.now().plusSeconds(reservationTtlSeconds));

        reservation = reservationRepository.save(reservation);

        ReservationItem reservationItem = new ReservationItem();
        reservationItem.setReservationId(reservation.getId());
        reservationItem.setProductId(product.getId());
        reservationItem.setQuantity(request.quantity());
        reservationItem = reservationItemRepository.save(reservationItem);

        int remaining = request.quantity();

        for (Inventory inventory : inventories) {

            if (remaining == 0) {
                break;
            }
            int availableInWarehouse = inventory.getAvailable();
            if (availableInWarehouse <= 0) {
                continue;
            }
            int allocated = Math.min(availableInWarehouse, remaining);

            // increase reserved stock
            inventory.setReserved(inventory.getReserved() + allocated);

            ReservationAllocation allocation = new ReservationAllocation();
            allocation.setReservationItemId(reservationItem.getId());
            allocation.setWarehouseId(inventory.getWarehouseId());
            allocation.setQuantity(allocated);
            reservationAllocationRepository.save(allocation);
            remaining -= allocated;
        }
        IdempotencyRecord idempotencyRecord = new IdempotencyRecord();
        idempotencyRecord.setTenantId(tenantId);
        idempotencyRecord.setUserId(userId);
        idempotencyRecord.setIdempotencyKey(idempotencyKey);
        idempotencyRecord.setRequestHash(requestHash);
        idempotencyRecord.setReservationId(reservation.getId());
        idempotencyRecordRepository.save(idempotencyRecord);

        return new ReservationResponse(reservation.getId(), reservation.getStatus(), reservation.getExpiresAt());
    }
}

