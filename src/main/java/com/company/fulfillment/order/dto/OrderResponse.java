package com.company.fulfillment.order.dto;

import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        String status
) {
}