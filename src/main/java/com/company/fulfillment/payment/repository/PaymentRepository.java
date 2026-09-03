package com.company.fulfillment.payment.repository;

import com.company.fulfillment.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Payment> findByOrderIdAndTenantId(UUID orderId, UUID tenantId);
}