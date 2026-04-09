package com.agora.config.seed;

import com.agora.config.seed.SeedUsersHelper.SeededUsers;
import com.agora.entity.reservation.Reservation;
import com.agora.entity.resource.Resource;
import com.agora.entity.user.User;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceRepository;
import com.agora.service.reservation.ReservationBookingReferenceService;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Réservations de démonstration : visibles sur {@code GET /api/calendar} (créneaux bloqués)
 * et cohérentes avec les comptes {@link SeedConstants} / ressources {@link SeedResourcesHelper}.
 */
final class SeedReservationsHelper {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationBookingReferenceService bookingReferenceService;

    SeedReservationsHelper(
            ReservationRepository reservationRepository,
            ResourceRepository resourceRepository,
            ReservationBookingReferenceService bookingReferenceService
    ) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.bookingReferenceService = bookingReferenceService;
    }

    void ensureSeedReservations(SeededUsers users) {
        // CONFIRMED — fête des voisins (créneau 09:00–10:00 occupé sur la Grande salle)
        ensure(
                users.user(),
                SeedConstants.RESOURCE_GRANDE_SALLE,
                LocalDate.of(2026, 4, 10),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                ReservationStatus.CONFIRMED,
                "Fête des voisins (démo seed)"
        );

        // CONFIRMED — association (Petite salle)
        ensure(
                users.assocManager(),
                SeedConstants.RESOURCE_PETITE_SALLE,
                LocalDate.of(2026, 4, 15),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                ReservationStatus.CONFIRMED,
                "Réunion bureau associatif (démo seed)"
        );

        // PENDING_VALIDATION — demande en attente (bloque le calendrier comme les autres statuts bloquants)
        ensure(
                users.staff(),
                SeedConstants.RESOURCE_GRANDE_SALLE,
                LocalDate.of(2026, 3, 25),
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                ReservationStatus.PENDING_VALIDATION,
                "Mairie — réunion interne (validation en cours)"
        );

        // CONFIRMED — réservation longue (08:00–11:00) : trois créneaux modèle bloqués
        ensure(
                users.admin(),
                SeedConstants.RESOURCE_CHAISES,
                LocalDate.of(2026, 4, 5),
                LocalTime.of(8, 0),
                LocalTime.of(11, 0),
                ReservationStatus.CONFIRMED,
                "Prêt matériel événement (démo seed)"
        );

        // CANCELLED — ne doit pas apparaître comme bloquant dans le calendrier
        ensure(
                users.user(),
                SeedConstants.RESOURCE_VIDEO_PROJECTEUR,
                LocalDate.of(2026, 4, 20),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                ReservationStatus.CANCELLED,
                "Ancienne demande annulée (démo — disponibilité rétablie)"
        );
    }

    private void ensure(
            User user,
            String resourceName,
            LocalDate date,
            LocalTime slotStart,
            LocalTime slotEnd,
            ReservationStatus status,
            String purpose
    ) {
        Resource resource = resourceRepository
                .findByNameIgnoreCase(resourceName)
                .orElseThrow(() -> new IllegalStateException("Ressource seed introuvable: " + resourceName));

        if (reservationRepository.existsByResource_IdAndReservationDateAndSlotStart(
                resource.getId(), date, slotStart)) {
            return;
        }

        Reservation r = new Reservation();
        r.setUser(user);
        r.setResource(resource);
        r.setReservationDate(date);
        r.setSlotStart(slotStart);
        r.setSlotEnd(slotEnd);
        r.setStatus(status);
        r.setPurpose(purpose);
        r.setBookingReference(bookingReferenceService.allocateNextReference(date));

        reservationRepository.save(r);
    }
}
