package com.company.fulfillment.events.controller;

import com.company.fulfillment.config.TenantContext;
import com.company.fulfillment.events.dto.DomainEventResponse;
import com.company.fulfillment.events.service.DomainEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class DomainEventController {

    private final DomainEventService domainEventService;

    public DomainEventController(DomainEventService domainEventService) {
        this.domainEventService = domainEventService;
    }

    @GetMapping
    public ResponseEntity<List<DomainEventResponse>> getEvents(
            @RequestHeader("X-Tenant-Id") UUID tenantId
    ) {
        TenantContext.validateTenant(tenantId);

        return ResponseEntity.ok(
                domainEventService.getEvents(tenantId)
        );
    }
}