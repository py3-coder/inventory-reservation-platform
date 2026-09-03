package com.company.fulfillment.payment;

import com.company.fulfillment.inventory.dto.ReservationRequest;
import com.company.fulfillment.inventory.dto.ReservationResponse;
import com.company.fulfillment.inventory.entity.Reservation;
import com.company.fulfillment.inventory.repository.ReservationRepository;
import com.company.fulfillment.inventory.service.ReservationService;
import com.company.fulfillment.order.entity.Order;
import com.company.fulfillment.order.repository.OrderRepository;
import com.company.fulfillment.payment.dto.PaymentCallbackRequest;
import com.company.fulfillment.payment.entity.Payment;
import com.company.fulfillment.payment.repository.PaymentRepository;
import com.company.fulfillment.payment.service.PaymentCallbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PaymentFailureTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private PaymentCallbackService paymentCallbackService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UUID tenantId =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final UUID userId =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final UUID warehouseId =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private UUID productId;
    private UUID inventoryId;
    private String sku;

    @BeforeEach
    void setup() {

        productId = UUID.randomUUID();
        inventoryId = UUID.randomUUID();
        sku = "PAYMENT-FAILURE-" + UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO product
                (
                    id,
                    tenant_id,
                    sku,
                    name,
                    description,
                    created_at,
                    updated_at
                )
            VALUES
                (?, ?, ?, ?, ?, now(), now())
            """,
                productId,
                tenantId,
                sku,
                "Payment Failure Test Product",
                "Test"
        );

        jdbcTemplate.update("""
            INSERT INTO inventory
                (
                    id,
                    tenant_id,
                    product_id,
                    warehouse_id,
                    on_hand,
                    reserved,
                    created_at,
                    updated_at
                )
            VALUES
                (?, ?, ?, ?, ?, ?, now(), now())
            """,
                inventoryId,
                tenantId,
                productId,
                warehouseId,
                5,
                0
        );
    }

    @Test
    void shouldReleaseStockWhenPaymentFails() {

        // ==================================================
        // 1. Reserve stock
        // ==================================================

        ReservationResponse reservationResponse =
                reservationService.reserve(
                        tenantId,
                        userId,
                        UUID.randomUUID().toString(),
                        new ReservationRequest(sku, 2)
                );

        UUID reservationId =
                reservationResponse.reservationId();

        assertNotNull(reservationId);

        // ==================================================
        // 2. Verify reservation consumed 2 units
        // ==================================================

        Integer reservedBeforeFailure =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reserved
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        Integer onHandBeforeFailure =
                jdbcTemplate.queryForObject(
                        """
                        SELECT on_hand
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        assertEquals(2, reservedBeforeFailure);
        assertEquals(5, onHandBeforeFailure);

        // ==================================================
        // 3. Create PAYMENT_PENDING order
        // ==================================================

        Order order = new Order();

        order.setTenantId(tenantId);
        order.setReservationId(reservationId);
        order.setUserId(userId);
        order.setStatus("PAYMENT_PENDING");

        order = orderRepository.saveAndFlush(order);

        UUID orderId = order.getId();

        assertNotNull(orderId);

        // ==================================================
        // 4. Create PENDING payment
        // ==================================================

        Payment payment = new Payment();

        payment.setTenantId(tenantId);
        payment.setOrderId(orderId);
        payment.setStatus("PENDING");

        payment = paymentRepository.saveAndFlush(payment);

        UUID paymentId = payment.getId();

        assertNotNull(paymentId);

        // ==================================================
        // 5. Send FAILURE callback
        // ==================================================

        PaymentCallbackRequest callback =
                new PaymentCallbackRequest(
                        paymentId,
                        orderId,
                        "FAILURE"
                );

        paymentCallbackService.handleCallback(
                tenantId,
                callback
        );

        // ==================================================
        // 6. Reservation must become CANCELLED
        // ==================================================

        Reservation finalReservation =
                reservationRepository
                        .findById(reservationId)
                        .orElseThrow();

        assertEquals(
                "CANCELLED",
                finalReservation.getStatus()
        );

        // ==================================================
        // 7. Payment must become FAILED
        // ==================================================

        Payment finalPayment =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow();

        assertEquals(
                "FAILED",
                finalPayment.getStatus()
        );

        // ==================================================
        // 8. Order must become FAILED
        // ==================================================

        Order finalOrder =
                orderRepository
                        .findById(orderId)
                        .orElseThrow();

        assertEquals(
                "FAILED",
                finalOrder.getStatus()
        );

        // ==================================================
        // 9. Reserved stock must be released
        // ==================================================

        Integer reservedAfterFailure =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reserved
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        Integer onHandAfterFailure =
                jdbcTemplate.queryForObject(
                        """
                        SELECT on_hand
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        assertEquals(0, reservedAfterFailure);
        assertEquals(5, onHandAfterFailure);

        // ==================================================
        // 10. Verify StockReleased event
        // ==================================================

        Integer stockReleasedEvents =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM domain_event
                        WHERE tenant_id = ?
                          AND event_type = 'StockReleased'
                          AND aggregate_id = ?
                        """,
                        Integer.class,
                        tenantId,
                        reservationId
                );

        assertEquals(1, stockReleasedEvents);
    }
}