package com.company.fulfillment.events.service;

import com.company.fulfillment.events.dto.DomainEventResponse;
import com.company.fulfillment.events.entity.DomainEvent;
import com.company.fulfillment.events.repository.DomainEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DomainEventService {

    private final DomainEventRepository domainEventRepository;

    public DomainEventService(
            DomainEventRepository domainEventRepository
    ) {
        this.domainEventRepository = domainEventRepository;
    }

    public void saveEvent(
            UUID tenantId,
            String eventType,
            UUID aggregateId
    ) {

        DomainEvent event = new DomainEvent();

        event.setTenantId(tenantId);
        event.setEventType(eventType);
        event.setAggregateId(aggregateId);

        switch (eventType) {

            case "StockReserved" -> {
                event.setAggregateType("Reservation");
                event.setPayload(
                        """
                        {"reservationId":"%s","status":"RESERVED"}
                        """.formatted(aggregateId)
                );
            }

            case "ReservationExpired" -> {
                event.setAggregateType("Reservation");
                event.setPayload(
                        """
                        {"reservationId":"%s","status":"EXPIRED"}
                        """.formatted(aggregateId)
                );
            }

            case "StockReleased" -> {
                event.setAggregateType("Reservation");
                event.setPayload(
                        """
                        {"reservationId":"%s","status":"STOCK_RELEASED"}
                        """.formatted(aggregateId)
                );
            }

            case "OrderConfirmed" -> {
                event.setAggregateType("Order");
                event.setPayload(
                        """
                        {"orderId":"%s","status":"CONFIRMED"}
                        """.formatted(aggregateId)
                );
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported domain event type: " + eventType
            );
        }

        domainEventRepository.save(event);
    }

    public List<DomainEventResponse> getEvents(UUID tenantId) {

        return domainEventRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(event -> new DomainEventResponse(
                        event.getId(),
                        event.getTenantId(),
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getPayload(),
                        event.getCreatedAt()
                ))
                .toList();
    }
}