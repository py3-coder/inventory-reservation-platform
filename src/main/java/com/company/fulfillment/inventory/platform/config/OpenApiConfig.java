package com.company.fulfillment.inventory.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Reservation & Order Fulfillment API")
                        .version("v1")
                        .description("""
                                REST API for multi-tenant inventory reservation
                                and order fulfillment.

                                The API supports product catalog management,
                                inventory management, stock reservations,
                                order creation, simulated payments,
                                tenant isolation, and domain event auditing.
                                """));
    }
}