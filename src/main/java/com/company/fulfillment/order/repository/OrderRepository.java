package com.company.fulfillment.order.repository;

import com.company.fulfillment.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Order> findByTenantIdAndReservationId(
            UUID tenantId,
            UUID reservationId
    );
}