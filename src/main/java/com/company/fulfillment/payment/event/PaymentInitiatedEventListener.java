package com.company.fulfillment.payment.event;

import com.company.fulfillment.payment.service.PaymentPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentInitiatedEventListener {

    private final PaymentPort paymentPort;

    public PaymentInitiatedEventListener(PaymentPort paymentPort) {
        this.paymentPort = paymentPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentInitiatedEvent event) {
        paymentPort.initiatePayment(
                event.paymentId(),
                event.orderId(),
                event.tenantId()
        );
    }
}