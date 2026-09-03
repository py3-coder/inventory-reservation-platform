package com.company.fulfillment.inventory.dto;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID reservationId,
        String status,
        Instant expiresAt
) {
}