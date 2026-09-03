package com.company.fulfillment.order.controller;

import com.company.fulfillment.config.TenantContext;
import com.company.fulfillment.order.dto.OrderRequest;
import com.company.fulfillment.order.dto.OrderResponse;
import com.company.fulfillment.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/order")
    public ResponseEntity<OrderResponse> processOrder(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody OrderRequest orderRequest){

        UUID userId = TenantContext.getUserId();
        OrderResponse orderResponse = orderService.processOrder(userId, tenantId, orderRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }
}
