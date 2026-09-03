package com.company.fulfillment.security;

import com.company.fulfillment.config.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.TimeZone;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class TenantIsolationTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private final UUID tenant1 =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final UUID tenant2 =
            UUID.randomUUID();

    private final UUID userId =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final UUID warehouseId =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private UUID productId;
    private UUID inventoryId;
    private String sku;

    private String tenant1AdminToken;

    @BeforeEach
    void setup() {

        /*
         * Create Tenant 2.
         */
        jdbcTemplate.update("""
            INSERT INTO tenant
                (id, name, created_at)
            VALUES
                (?, ?, now())
            ON CONFLICT (id) DO NOTHING
            """,
                tenant2,
                "Tenant 2"
        );

        /*
         * Create a product belonging to Tenant 1.
         */
        productId = UUID.randomUUID();
        inventoryId = UUID.randomUUID();
        sku = "TENANT-ISOLATION-" + UUID.randomUUID();

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
                tenant1,
                sku,
                "Tenant Isolation Product",
                "Tenant 1 product"
        );

        /*
         * Create inventory belonging to Tenant 1.
         */
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
                tenant1,
                productId,
                warehouseId,
                5,
                0
        );

        /*
         * Generate a valid Tenant 1 ADMIN JWT.
         *
         * The request will later deliberately send tenant2
         * in X-Tenant-Id to verify cross-tenant access is rejected.
         */
        tenant1AdminToken = jwtService.generateToken(
                userId,
                tenant1,
                "ADMIN"
        );
    }

    @Test
    void tenantTwoMustNotAccessTenantOneInventory() throws Exception {

        /*
         * The JWT belongs to Tenant 1.
         *
         * X-Tenant-Id deliberately claims Tenant 2.
         *
         * TenantContext.validateTenant() must reject this mismatch
         * with HTTP 403.
         */
        mockMvc.perform(
                        get("/api/v1/admin/inventory/{productId}/{warehouseId}",
                                productId,
                                warehouseId
                        )
                                .header(
                                        "X-Tenant-Id",
                                        tenant2.toString()
                                )
                                .header(
                                        "Authorization",
                                        "Bearer " + tenant1AdminToken
                                )
                )
                .andExpect(status().isForbidden());
    }
}