package com.company.fulfillment.inventory.repository;

import com.company.fulfillment.inventory.entity.ReservationAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationAllocationRepository
        extends JpaRepository<ReservationAllocation, UUID> {

    List<ReservationAllocation> findByReservationItemId(
            UUID reservationItemId
    );
}