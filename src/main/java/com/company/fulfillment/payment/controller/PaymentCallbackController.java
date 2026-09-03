package com.company.fulfillment.payment.controller;

import com.company.fulfillment.payment.dto.PaymentCallbackRequest;
import com.company.fulfillment.payment.service.PaymentCallbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentCallbackController {

    private final PaymentCallbackService paymentCallbackService;

    public PaymentCallbackController(PaymentCallbackService paymentCallbackService) {
        this.paymentCallbackService = paymentCallbackService;
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody PaymentCallbackRequest request) {

        paymentCallbackService.handleCallback(tenantId, request);
        return ResponseEntity.ok().build();
    }
}