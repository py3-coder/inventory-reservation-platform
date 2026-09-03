package com.company.fulfillment.events;

import com.company.fulfillment.config.JwtService;
import com.company.fulfillment.events.service.DomainEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.TimeZone;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DomainEventReadTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DomainEventService domainEventService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UUID tenant1 =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final UUID userId =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldReturnOnlyEventsForCurrentTenant() throws Exception {

        UUID reservationId = UUID.randomUUID();

        domainEventService.saveEvent(
                tenant1,
                "StockReserved",
                reservationId
        );

        String token = jwtService.generateToken(
                userId,
                tenant1,
                "USER"
        );

        mockMvc.perform(
                        get("/api/v1/events")
                                .header("X-Tenant-Id", tenant1.toString())
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$",
                        hasSize(org.hamcrest.Matchers.greaterThan(0))
                ))
                .andExpect(jsonPath(
                        "$[*].eventType",
                        hasItem("StockReserved")
                ))
                .andExpect(jsonPath(
                        "$[*].aggregateId",
                        hasItem(reservationId.toString())
                ));
    }

    @Test
    void tenantMismatchMustReturn403() throws Exception {

        UUID tenant2 = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO tenant
                (id, name, created_at)
            VALUES
                (?, ?, now())
            ON CONFLICT (id) DO NOTHING
            """,
                tenant2,
                "Event Test Tenant 2"
        );

        String token = jwtService.generateToken(
                userId,
                tenant1,
                "USER"
        );

        mockMvc.perform(
                        get("/api/v1/events")
                                .header("X-Tenant-Id", tenant2.toString())
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isForbidden());
    }
}