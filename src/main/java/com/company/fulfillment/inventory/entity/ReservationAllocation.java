package com.company.fulfillment.inventory.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "reservation_allocation")
public class ReservationAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reservation_item_id", nullable = false)
    private UUID reservationItemId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(nullable = false)
    private int quantity;

    public UUID getId() {
        return id;
    }

    public UUID getReservationItemId() {
        return reservationItemId;
    }

    public void setReservationItemId(UUID reservationItemId) {
        this.reservationItemId = reservationItemId;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}