package com.company.fulfillment.inventory.controller;

import com.company.fulfillment.config.TenantContext;
import com.company.fulfillment.inventory.dto.ReservationRequest;
import com.company.fulfillment.inventory.dto.ReservationResponse;
import com.company.fulfillment.inventory.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }


    @PostMapping("/reservation")
    public ResponseEntity<ReservationResponse> reserve(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReservationRequest request){

        UUID jwtTenantId = TenantContext.getTenantId();

        TenantContext.validateTenant(tenantId);
        UUID userId = TenantContext.getUserId();
        ReservationResponse response = reservationService.reserve(tenantId,userId , idempotencyKey, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }


}
