package com.company.fulfillment.inventory.service;

import com.company.fulfillment.catalog.entity.Product;
import com.company.fulfillment.catalog.repository.ProductRepository;
import com.company.fulfillment.events.service.DomainEventService;
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
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReservationService {

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final ReservationAllocationRepository reservationAllocationRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final DomainEventService domainEventService;

    private static final ConcurrentHashMap<String, Object> IDEMPOTENCY_LOCKS =
            new ConcurrentHashMap<>();

    @Value("${app.reservation.ttl-seconds}")
    private long reservationTtlSeconds;

    public ReservationService(
            InventoryRepository inventoryRepository,
            ReservationRepository reservationRepository,
            ProductRepository productRepository,
            ReservationItemRepository reservationItemRepository,
            ReservationAllocationRepository reservationAllocationRepository,
            IdempotencyRecordRepository idempotencyRecordRepository, DomainEventService domainEventService
    ) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.productRepository = productRepository;
        this.reservationItemRepository = reservationItemRepository;
        this.reservationAllocationRepository = reservationAllocationRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.domainEventService = domainEventService;
    }

    @Transactional
    public ReservationResponse reserve(
            UUID tenantId,
            UUID userId,
            String idempotencyKey,
            ReservationRequest request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Reservation request is required");
        }
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (request.sku() == null || request.sku().isBlank()) {
            throw new IllegalArgumentException("SKU is required");
        }
        String requestHash = DigestUtils.sha256Hex(
                request.sku() + ":" + request.quantity()
        );

        String lockKey = tenantId + ":" + userId + ":" + idempotencyKey;
        Object lock = IDEMPOTENCY_LOCKS.computeIfAbsent(
                lockKey,
                key -> new Object()
        );

        synchronized (lock) {
            try {
                Optional<IdempotencyRecord> existing =
                        idempotencyRecordRepository
                                .findByTenantIdAndUserIdAndIdempotencyKey(
                                        tenantId,
                                        userId,
                                        idempotencyKey
                                );

                if (existing.isPresent()) {
                    IdempotencyRecord record = existing.get();
                    if (!record.getRequestHash().equals(requestHash)) {
                        throw new IllegalArgumentException(
                                "Idempotency key reused with different request"
                        );
                    }

                    Reservation reservation =
                            reservationRepository
                                    .findById(record.getReservationId())
                                    .orElseThrow(() ->
                                            new IllegalStateException(
                                                    "Reservation referenced by idempotency record was not found"
                                            )
                                    );

                    if (!tenantId.equals(reservation.getTenantId())) {
                        throw new IllegalStateException(
                                "Reservation does not belong to tenant"
                        );
                    }

                    return new ReservationResponse(
                            reservation.getId(),
                            reservation.getStatus(),
                            reservation.getExpiresAt()
                    );
                }

                Product product =
                        productRepository
                                .findByTenantIdAndSku(tenantId, request.sku())
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Product not found"
                                        )
                                );

                List<Inventory> inventories =
                        inventoryRepository.findForUpdate(
                                tenantId,
                                product.getId()
                        );

                if (inventories.isEmpty()) {
                    throw new IllegalArgumentException(
                            "No inventory found for product"
                    );
                }

                int available = inventories.stream()
                        .mapToInt(Inventory::getAvailable)
                        .sum();

                if (available < request.quantity()) {
                    throw new IllegalArgumentException(
                            "Insufficient stock"
                    );
                }

                Reservation reservation = new Reservation();
                reservation.setTenantId(tenantId);
                reservation.setUserId(userId);
                reservation.setStatus("RESERVED");
                reservation.setExpiresAt(Instant.now().plusSeconds(reservationTtlSeconds));
                reservation = reservationRepository.save(reservation);

                domainEventService.saveEvent(
                        tenantId,
                        "StockReserved",
                        reservation.getId()
                );
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

                    inventory.setReserved(inventory.getReserved() + allocated);
                    ReservationAllocation allocation = new ReservationAllocation();
                    allocation.setReservationItemId(reservationItem.getId());
                    allocation.setWarehouseId(inventory.getWarehouseId());
                    allocation.setQuantity(allocated);
                    reservationAllocationRepository.save(allocation);
                    remaining -= allocated;
                }
                if (remaining > 0) {
                    throw new IllegalStateException(
                            "Unable to allocate requested stock"
                    );
                }
                IdempotencyRecord idempotencyRecord = new IdempotencyRecord();

                idempotencyRecord.setTenantId(tenantId);
                idempotencyRecord.setUserId(userId);
                idempotencyRecord.setIdempotencyKey(idempotencyKey);
                idempotencyRecord.setRequestHash(requestHash);
                idempotencyRecord.setReservationId(reservation.getId());

                idempotencyRecordRepository.save(idempotencyRecord);

                return new ReservationResponse(
                        reservation.getId(),
                        reservation.getStatus(),
                        reservation.getExpiresAt()
                );

            } finally {
                IDEMPOTENCY_LOCKS.remove(lockKey, lock);
            }
        }
    }
}
