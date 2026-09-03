package com.company.fulfillment.inventory.repository;

import com.company.fulfillment.inventory.entity.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM Reservation r WHERE r.id = :id AND r.tenantId = :tenantId
    """)
    Optional<Reservation> findForUpdate(
            @Param("id") UUID id,
            @Param("tenantId") UUID tenantId
    );

    @Query("""
    SELECT r  FROM Reservation r WHERE r.status = 'RESERVED' AND r.expiresAt <= :now
    """)
    List<Reservation> findExpiredReservations(
            @Param("now") Instant now
    );
}