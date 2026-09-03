package com.company.fulfillment.inventory.repository;

import com.company.fulfillment.inventory.entity.ReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationItemRepository
        extends JpaRepository<ReservationItem, UUID> {

    List<ReservationItem> findByReservationId(UUID reservationId);
}