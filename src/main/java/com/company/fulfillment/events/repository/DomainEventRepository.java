package com.company.fulfillment.events.repository;

import com.company.fulfillment.events.entity.DomainEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DomainEventRepository
        extends JpaRepository<DomainEvent, UUID> {

    List<DomainEvent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}