package com.company.fulfillment.idempotency.repository;

import com.company.fulfillment.idempotency.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByTenantIdAndUserIdAndIdempotencyKey(
            UUID tenantId,
            UUID userId,
            String idempotencyKey
    );
}