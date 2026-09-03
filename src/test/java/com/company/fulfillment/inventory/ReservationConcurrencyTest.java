package com.company.fulfillment.inventory;

import com.company.fulfillment.inventory.dto.ReservationRequest;
import com.company.fulfillment.inventory.dto.ReservationResponse;
import com.company.fulfillment.inventory.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.*;

        import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ReservationConcurrencyTest {

    static {
        TimeZone.setDefault(
                TimeZone.getTimeZone("UTC")
        );
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

        sku = "CONCURRENCY-" + UUID.randomUUID();

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
                "Concurrency Test Product",
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
    void shouldAllowOnlyFiveReservationsWhenStockIsFive()
            throws Exception {

        int numberOfRequests = 20;

        ExecutorService executor =
                Executors.newFixedThreadPool(numberOfRequests);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        List<Future<Boolean>> futures =
                new ArrayList<>();

        for (int i = 0; i < numberOfRequests; i++) {

            futures.add(
                    executor.submit(() -> {
                        // Wait until all threads are ready
                        startLatch.await();
                        try {
                            ReservationResponse response =
                                    reservationService.reserve(tenantId,
                                            userId,
                                            UUID.randomUUID().toString(),
                                            new ReservationRequest(
                                                    sku,
                                                    1
                                            )
                                    );
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    })
            );
        }

        // Release all threads approximately together
        startLatch.countDown();

        int successful = 0;
        int failed = 0;

        for (Future<Boolean> future : futures) {

            if (future.get()) {
                successful++;
            } else {
                failed++;
            }
        }

        executor.shutdown();

        // Exactly 5 reservations should succeed
        assertEquals(5, successful);

        // Remaining 15 should fail
        assertEquals(15, failed);

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

        assertEquals(5, reserved);
        assertEquals(5, onHand);

        int available = onHand - reserved;

        assertEquals(0, available);
    }
}