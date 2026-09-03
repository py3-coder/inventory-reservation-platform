package com.company.fulfillment.payment.service;

import com.company.fulfillment.payment.dto.PaymentCallbackRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.UUID;

@Component
public class FakePaymentAdapter implements PaymentPort {

    @Value("${payment.fake.outcome}")
    private String outcome;

    @Value("${payment.fake.delay-seconds}")
    private long delaySeconds;

    private final TaskScheduler taskScheduler;
    private final RestClient restClient;

    public FakePaymentAdapter(
            TaskScheduler taskScheduler,
            RestClient.Builder restClientBuilder
    ) {
        this.taskScheduler = taskScheduler;
        this.restClient =
                restClientBuilder
                        .baseUrl("http://localhost:8080")
                        .build();
    }

    @Override
    public void initiatePayment(UUID paymentId, UUID orderId, UUID tenantId) {

        long delaySecond = delaySeconds;
        taskScheduler.schedule(
                () -> sendCallback(paymentId, orderId, tenantId),
                Instant.now().plusSeconds(delaySecond)
        );
    }

    private void sendCallback(UUID paymentId, UUID orderId, UUID tenantId) {

        restClient.post()
                .uri("/api/v1/payments/callback")
                .header("X-Tenant-Id", tenantId.toString())
                .body(new PaymentCallbackRequest(
                        paymentId,
                        orderId,
                        outcome
                ))
                .retrieve()
                .toBodilessEntity();
    }
}