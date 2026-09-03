package com.company.fulfillment.events.dto;

import java.util.UUID;

public record ReservationExpiredEvent(
        UUID tenantId,
        UUID reservationId
) {}