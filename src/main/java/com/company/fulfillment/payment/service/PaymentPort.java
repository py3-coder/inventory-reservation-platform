package com.company.fulfillment.payment.service;

import java.util.UUID;

public interface PaymentPort {

    void initiatePayment(UUID paymentId, UUID orderId, UUID tenantId);
}