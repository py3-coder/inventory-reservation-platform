package com.company.fulfillment.inventory;

import com.company.fulfillment.inventory.dto.ReservationRequest;
import com.company.fulfillment.inventory.dto.ReservationResponse;
import com.company.fulfillment.inventory.entity.Reservation;
import com.company.fulfillment.inventory.repository.ReservationRepository;
import com.company.fulfillment.inventory.service.ReservationExpiryService;
import com.company.fulfillment.inventory.service.ReservationService;
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
class ReservationExpiryTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationExpiryService reservationExpiryService;

    @Autowired
    private ReservationRepository reservationRepository;

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
        sku = "EXPIRY-" + UUID.randomUUID();

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
                "Expiry Test Product",
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
    void shouldExpireReservationAndReleaseStock() {

        ReservationResponse response =
                reservationService.reserve(
                        tenantId,
                        userId,
                        UUID.randomUUID().toString(),
                        new ReservationRequest(sku, 2)
                );

        assertNotNull(response);
        assertNotNull(response.reservationId());

        Integer reservedBefore =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reserved
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        assertEquals(2, reservedBefore);

        Reservation reservation =
                reservationRepository
                        .findById(response.reservationId())
                        .orElseThrow();

        // Force expiry for deterministic testing.
        reservation.setExpiresAt(
                Instant.now().minusSeconds(10)
        );

        reservationRepository.saveAndFlush(reservation);

        reservationExpiryService.expireReservation(
                response.reservationId(),
                tenantId
        );

        Reservation expiredReservation =
                reservationRepository
                        .findById(response.reservationId())
                        .orElseThrow();

        assertEquals(
                "EXPIRED",
                expiredReservation.getStatus()
        );

        Integer reservedAfter = jdbcTemplate.queryForObject(
                        """
                        SELECT reserved
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        Integer onHand = jdbcTemplate.queryForObject(
                        """
                        SELECT on_hand
                        FROM inventory
                        WHERE id = ?
                        """,
                        Integer.class,
                        inventoryId
                );

        assertEquals(0, reservedAfter);
        assertEquals(5, onHand);
        assertEquals(5, onHand - reservedAfter);
    }
}