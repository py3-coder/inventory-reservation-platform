package com.company.fulfillment.inventory;

import com.company.fulfillment.inventory.dto.ReservationRequest;
import com.company.fulfillment.inventory.dto.ReservationResponse;
import com.company.fulfillment.inventory.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReservationIdempotencyTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired
    private ReservationService reservationService;

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
        sku = "IDEMPOTENCY-" + UUID.randomUUID();

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
                "Idempotency Test Product",
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
    void shouldReturnSameReservationForSameIdempotencyKey() {

        String idempotencyKey =
                "IDEMPOTENCY-KEY-" + UUID.randomUUID();

        ReservationRequest request =
                new ReservationRequest(sku, 1);

        // ==================================================
        // First request
        // ==================================================

        ReservationResponse firstResponse =
                reservationService.reserve(
                        tenantId,
                        userId,
                        idempotencyKey,
                        request
                );

        assertNotNull(firstResponse);
        assertNotNull(firstResponse.reservationId());

        UUID firstReservationId =
                firstResponse.reservationId();

        // ==================================================
        // Second request with SAME idempotency key
        // ==================================================

        ReservationResponse secondResponse =
                reservationService.reserve(
                        tenantId,
                        userId,
                        idempotencyKey,
                        request
                );

        assertNotNull(secondResponse);
        assertNotNull(secondResponse.reservationId());

        UUID secondReservationId =
                secondResponse.reservationId();

        // ==================================================
        // Both requests must return SAME reservation
        // ==================================================

        assertEquals(
                firstReservationId,
                secondReservationId,
                "Same idempotency key must return the same reservation"
        );

        // ==================================================
        // Verify exactly ONE reservation is associated
        // with THIS idempotency key.
        //
        // Do NOT count all reservations because other
        // integration tests share the same database.
        // ==================================================

        Integer idempotencyRecords =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM idempotency_record
                        WHERE tenant_id = ?
                          AND user_id = ?
                          AND idempotency_key = ?
                        """,
                        Integer.class,
                        tenantId,
                        userId,
                        idempotencyKey
                );

        assertNotNull(idempotencyRecords);

        assertEquals(
                1,
                idempotencyRecords,
                "Exactly one idempotency record must exist"
        );

        // ==================================================
        // Verify that the idempotency record points to the
        // SAME reservation returned by both requests.
        // ==================================================

        UUID storedReservationId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reservation_id
                        FROM idempotency_record
                        WHERE tenant_id = ?
                          AND user_id = ?
                          AND idempotency_key = ?
                        """,
                        UUID.class,
                        tenantId,
                        userId,
                        idempotencyKey
                );

        assertEquals(
                firstReservationId,
                storedReservationId,
                "Idempotency record must point to the returned reservation"
        );

        // ==================================================
        // Stock must only be reserved ONCE
        // ==================================================

        Integer reserved =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reserved
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        Integer onHand =
                jdbcTemplate.queryForObject(
                        """
                        SELECT on_hand
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        assertNotNull(reserved);
        assertNotNull(onHand);

        assertEquals(
                1,
                reserved,
                "Stock must only be reserved once"
        );

        assertEquals(
                5,
                onHand,
                "on_hand must not change during reservation"
        );

        assertEquals(
                4,
                onHand - reserved,
                "Available stock must be 4"
        );
    }
}