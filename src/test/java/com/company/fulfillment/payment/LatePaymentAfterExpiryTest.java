package com.company.fulfillment.payment;

import com.company.fulfillment.inventory.dto.ReservationRequest;
import com.company.fulfillment.inventory.dto.ReservationResponse;
import com.company.fulfillment.inventory.entity.Reservation;
import com.company.fulfillment.inventory.repository.ReservationRepository;
import com.company.fulfillment.inventory.service.ReservationExpiryService;
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
class LatePaymentAfterExpiryTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationExpiryService reservationExpiryService;

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
        sku = "LATE-PAYMENT-" + UUID.randomUUID();

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
                "Late Payment Test Product",
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
    void shouldOrphanLatePaymentAfterReservationExpiry() {

        // ==================================================
        // 1. Reserve 2 units
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

        Integer reservedBeforeExpiry =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reserved
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        assertEquals(2, reservedBeforeExpiry);

        // ==================================================
        // 2. Force reservation expiry
        // ==================================================

        Reservation reservation =
                reservationRepository
                        .findById(reservationId)
                        .orElseThrow();

        reservation.setExpiresAt(
                Instant.now().minusSeconds(10)
        );

        reservationRepository.saveAndFlush(reservation);

        // ==================================================
        // 3. Expire reservation
        // ==================================================

        reservationExpiryService.expireReservation(
                reservationId,
                tenantId
        );

        Reservation expiredReservation =
                reservationRepository
                        .findById(reservationId)
                        .orElseThrow();

        assertEquals(
                "EXPIRED",
                expiredReservation.getStatus()
        );

        // ==================================================
        // 4. Verify stock was released
        // ==================================================

        Integer reservedAfterExpiry =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reserved
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        Integer onHandAfterExpiry =
                jdbcTemplate.queryForObject(
                        """
                        SELECT on_hand
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        assertEquals(0, reservedAfterExpiry);
        assertEquals(5, onHandAfterExpiry);

        // ==================================================
        // 5. Simulate an already-created PAYMENT_PENDING order
        //
        // We cannot use OrderService here because it should
        // reject an expired reservation.
        //
        // Instead, create the historical order state through
        // the JPA repository.
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
        // 6. Create pending payment
        // ==================================================

        Payment payment = new Payment();

        payment.setTenantId(tenantId);
        payment.setOrderId(orderId);
        payment.setStatus("PENDING");

        payment = paymentRepository.saveAndFlush(payment);

        UUID paymentId = payment.getId();

        assertNotNull(paymentId);

        // ==================================================
        // 7. Verify setup
        // ==================================================

        Order pendingOrder =
                orderRepository
                        .findById(orderId)
                        .orElseThrow();

        Payment pendingPayment =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow();

        assertEquals(
                "PAYMENT_PENDING",
                pendingOrder.getStatus()
        );

        assertEquals(
                "PENDING",
                pendingPayment.getStatus()
        );

        assertEquals(
                reservationId,
                pendingOrder.getReservationId()
        );

        assertEquals(
                orderId,
                pendingPayment.getOrderId()
        );

        // ==================================================
        // 8. Send late SUCCESS payment callback
        // ==================================================

        PaymentCallbackRequest callback =
                new PaymentCallbackRequest(
                        paymentId,
                        orderId,
                        "SUCCESS"
                );

        paymentCallbackService.handleCallback(
                tenantId,
                callback
        );

        // ==================================================
        // 9. Payment must become ORPHANED
        // ==================================================

        Payment finalPayment =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow();

        assertEquals(
                "ORPHANED",
                finalPayment.getStatus()
        );

        // ==================================================
        // 10. Order must NOT be confirmed
        // ==================================================

        Order finalOrder =
                orderRepository
                        .findById(orderId)
                        .orElseThrow();

        assertEquals(
                "PAYMENT_PENDING",
                finalOrder.getStatus()
        );

        assertNotEquals(
                "CONFIRMED",
                finalOrder.getStatus()
        );

        // ==================================================
        // 11. Reservation must remain EXPIRED
        // ==================================================

        Reservation finalReservation =
                reservationRepository
                        .findById(reservationId)
                        .orElseThrow();

        assertEquals(
                "EXPIRED",
                finalReservation.getStatus()
        );

        // ==================================================
        // 12. Stock must NOT be deducted again
        // ==================================================

        Integer finalReserved =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reserved
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        Integer finalOnHand =
                jdbcTemplate.queryForObject(
                        """
                        SELECT on_hand
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        assertEquals(0, finalReserved);
        assertEquals(5, finalOnHand);
    }
}