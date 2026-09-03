package com.company.fulfillment.events.repository;

import com.company.fulfillment.events.entity.DomainEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DomainEventRepository
        extends JpaRepository<DomainEvent, UUID> {
}