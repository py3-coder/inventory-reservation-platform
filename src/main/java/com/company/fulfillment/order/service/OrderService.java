package com.company.fulfillment.order.service;

import com.company.fulfillment.inventory.entity.Reservation;
import com.company.fulfillment.inventory.repository.ReservationRepository;
import com.company.fulfillment.order.dto.OrderRequest;
import com.company.fulfillment.order.dto.OrderResponse;
import com.company.fulfillment.order.entity.Order;
import com.company.fulfillment.order.repository.OrderRepository;
import com.company.fulfillment.payment.entity.Payment;
import com.company.fulfillment.payment.event.PaymentInitiatedEvent;
import com.company.fulfillment.payment.repository.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private final ReservationRepository reservationRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            ReservationRepository reservationRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.reservationRepository = reservationRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResponse processOrder(UUID userId, UUID tenantId, OrderRequest orderRequest) {
        Reservation reservation = reservationRepository
                .findForUpdate(orderRequest.reservationId(), tenantId)
                .orElseThrow(() ->
                        new RuntimeException("Reservation not found")
                );
        if (!reservation.getUserId().equals(userId)) {
            throw new RuntimeException(
                    "Reservation does not belong to user"
            );
        }
        if (!"RESERVED".equals(reservation.getStatus())) {
            throw new RuntimeException(
                    "Reservation is not available"
            );
        }
        if (!reservation.getExpiresAt().isAfter(Instant.now())) {
            throw new RuntimeException(
                    "Reservation expired"
            );
        }
        Optional<Order> existingOrder =
                orderRepository.findByTenantIdAndReservationId(
                        tenantId,
                        orderRequest.reservationId()
                );

        if (existingOrder.isPresent()) {
            return new OrderResponse(
                    existingOrder.get().getId(),
                    existingOrder.get().getStatus()
            );
        }

        Order order = new Order();
        order.setStatus("PAYMENT_PENDING");
        order.setReservationId(orderRequest.reservationId());
        order.setUserId(userId);
        order.setTenantId(tenantId);
        order = orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setStatus("PENDING");
        payment.setTenantId(tenantId);
        paymentRepository.save(payment);
        eventPublisher.publishEvent(
                new PaymentInitiatedEvent(payment.getId(), order.getId(), tenantId)
        );

        return new OrderResponse(order.getId(), order.getStatus()
        );
    }
}