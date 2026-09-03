package com.company.fulfillment.inventory.scheduler;

import com.company.fulfillment.inventory.entity.Reservation;
import com.company.fulfillment.inventory.repository.ReservationRepository;
import com.company.fulfillment.inventory.service.ReservationExpiryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ReservationExpiryScheduler {

    private final ReservationRepository reservationRepository;
    private final ReservationExpiryService expiryService;

    public ReservationExpiryScheduler(ReservationRepository reservationRepository, ReservationExpiryService expiryService) {
        this.reservationRepository = reservationRepository;
        this.expiryService = expiryService;
    }
    @Scheduled(fixedDelayString = "${reservation.expiry.scheduler-delay-ms}")
    public void expireReservations() {
        List<Reservation> expired = reservationRepository.findExpiredReservations(Instant.now());
        for (Reservation reservation : expired) {
            expiryService.expireReservation(reservation.getId(), reservation.getTenantId());
        }
    }
}