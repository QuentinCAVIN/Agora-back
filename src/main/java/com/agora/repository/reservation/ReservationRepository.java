package com.agora.repository.reservation;

import com.agora.entity.reservation.Reservation;
import com.agora.enums.reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Query("""
            select (count(r) > 0)
            from Reservation r
            where r.resource.id = :resourceId
              and r.reservationDate = :date
              and r.status in :activeStatuses
              and r.slotStart < :slotEnd
              and r.slotEnd > :slotStart
            """)
    boolean existsOverlappingSlot(
            UUID resourceId,
            LocalDate date,
            LocalTime slotStart,
            LocalTime slotEnd,
            List<ReservationStatus> activeStatuses
    );
}
