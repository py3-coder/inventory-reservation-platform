package com.company.fulfillment.payment.service;

import com.company.fulfillment.events.service.DomainEventService;
import com.company.fulfillment.inventory.entity.Inventory;
import com.company.fulfillment.inventory.entity.Reservation;
import com.company.fulfillment.inventory.entity.ReservationAllocation;
import com.company.fulfillment.inventory.entity.ReservationItem;
import com.company.fulfillment.inventory.repository.InventoryRepository;
import com.company.fulfillment.inventory.repository.ReservationAllocationRepository;
import com.company.fulfillment.inventory.repository.ReservationItemRepository;
import com.company.fulfillment.inventory.repository.ReservationRepository;
import com.company.fulfillment.order.entity.Order;
import com.company.fulfillment.order.repository.OrderRepository;
import com.company.fulfillment.payment.dto.PaymentCallbackRequest;
import com.company.fulfillment.payment.entity.Payment;
import com.company.fulfillment.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentCallbackService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final ReservationAllocationRepository allocationRepository;
    private final InventoryRepository inventoryRepository;
    private final DomainEventService domainEventService;

    public PaymentCallbackService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            ReservationRepository reservationRepository,
            ReservationItemRepository reservationItemRepository,
            ReservationAllocationRepository allocationRepository,
            InventoryRepository inventoryRepository,
            DomainEventService domainEventService
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.reservationItemRepository = reservationItemRepository;
        this.allocationRepository = allocationRepository;
        this.inventoryRepository = inventoryRepository;
        this.domainEventService = domainEventService;
    }

    @Transactional
    public void handleCallback(UUID tenantId, PaymentCallbackRequest request) {

        Payment payment = paymentRepository
                .findByIdAndTenantId(
                        request.paymentId(),
                        tenantId
                )
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        if (!payment.getOrderId().equals(request.orderId())) {
            throw new RuntimeException(
                    "Payment does not belong to order"
            );
        }
        if ("SUCCESS".equals(payment.getStatus())
                || "FAILED".equals(payment.getStatus())
                || "ORPHANED".equals(payment.getStatus())) {
            return;
        }

        if (!"PENDING".equals(payment.getStatus())) {
            return;
        }
        Order order = orderRepository
                .findByIdAndTenantId(
                        request.orderId(),
                        tenantId
                )
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        if (!"PAYMENT_PENDING".equals(order.getStatus())) {
            return;
        }

        Reservation reservation = reservationRepository
                .findForUpdate(
                        order.getReservationId(),
                        tenantId
                )
                .orElseThrow(() ->
                        new RuntimeException("Reservation not found"));

        if ("FAILURE".equals(request.status())) {
            if ("RESERVED".equals(reservation.getStatus())) {
                releaseReservedStock(
                        tenantId,
                        reservation
                );
                reservation.setStatus("CANCELLED");
            }
            order.setStatus("FAILED");
            payment.setStatus("FAILED");
            domainEventService.saveEvent(
                    tenantId,
                    "StockReleased",
                    reservation.getId()
            );
            return;
        }

        if ("TIMEOUT".equals(request.status())) {
            if ("RESERVED".equals(reservation.getStatus())) {
                releaseReservedStock(
                        tenantId,
                        reservation
                );

                reservation.setStatus("CANCELLED");
            }
            order.setStatus("FAILED");
            payment.setStatus("TIMEOUT");

            domainEventService.saveEvent(
                    tenantId,
                    "StockReleased",
                    reservation.getId()
            );
            return;
        }
        if ("SUCCESS".equals(request.status())) {
            if (!"RESERVED".equals(reservation.getStatus())) {
                payment.setStatus("ORPHANED");
                return;
            }

            if (!reservation.getExpiresAt().isAfter(Instant.now())) {
                payment.setStatus("ORPHANED");
                return;
            }
            List<ReservationItem> items = reservationItemRepository
                            .findByReservationId(reservation.getId());

            for (ReservationItem item : items) {
                List<ReservationAllocation> allocations = allocationRepository
                                .findByReservationItemId(item.getId());

                for (ReservationAllocation allocation : allocations) {
                    Inventory inventory = inventoryRepository
                                    .findForUpdateByWarehouse(
                                            tenantId,
                                            item.getProductId(),
                                            allocation.getWarehouseId()
                                    )
                                    .orElseThrow(() -> new RuntimeException("Inventory not found"));

                    int quantity = allocation.getQuantity();

                    if (inventory.getReserved() < quantity) {
                        throw new RuntimeException(
                                "Insufficient reserved stock"
                        );
                    }
                    if (inventory.getOnHand() < quantity) {
                        throw new RuntimeException("Insufficient on-hand stock");
                    }

                    inventory.setReserved(inventory.getReserved() - quantity);
                    inventory.setOnHand(inventory.getOnHand() - quantity);
                }
            }
            reservation.setStatus("CONFIRMED");
            order.setStatus("CONFIRMED");
            payment.setStatus("SUCCESS");

            domainEventService.saveEvent(
                    tenantId,
                    "OrderConfirmed",
                    order.getId()
            );
            return;
        }

        throw new RuntimeException("Unsupported payment status: " + request.status()
        );
    }

    private void releaseReservedStock(
            UUID tenantId,
            Reservation reservation
    ) {

        List<ReservationItem> items = reservationItemRepository
                        .findByReservationId(reservation.getId());

        for (ReservationItem item : items) {

            List<ReservationAllocation> allocations = allocationRepository
                            .findByReservationItemId(item.getId());

            for (ReservationAllocation allocation : allocations) {
                Inventory inventory = inventoryRepository
                                .findForUpdateByWarehouse(
                                        tenantId,
                                        item.getProductId(),
                                        allocation.getWarehouseId()
                                )
                                .orElseThrow(() ->
                                        new RuntimeException(
                                                "Inventory not found"
                                        ));

                int quantity = allocation.getQuantity();
                if (inventory.getReserved() < quantity) {
                    throw new RuntimeException(
                            "Reserved stock cannot become negative"
                    );
                }

                inventory.setReserved(inventory.getReserved() - quantity
                );
            }
        }
    }
}