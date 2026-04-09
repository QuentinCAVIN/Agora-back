package com.agora.repository.reservation;

import com.agora.entity.reservation.Reservation;
import com.agora.enums.reservation.DepositStatus;
import com.agora.enums.reservation.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID>, JpaSpecificationExecutor<Reservation> {

    void deleteByUser_Id(UUID userId);

    long countByReservationDate(LocalDate reservationDate);

    long countByStatus(ReservationStatus status);

    long countByDepositStatus(DepositStatus depositStatus);

    Page<Reservation> findByUser_Id(UUID userId, Pageable pageable);

    Page<Reservation> findByUser_IdAndStatus(UUID userId, ReservationStatus status, Pageable pageable);

    Optional<Reservation> findByIdAndUser_Id(UUID id, UUID userId);

    boolean existsByResource_IdAndReservationDateAndSlotStart(
            UUID resourceId,
            LocalDate reservationDate,
            LocalTime slotStart
    );

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

    @Query("""
            select r from Reservation r
            join fetch r.resource res
            where res.id in :resourceIds
              and r.reservationDate >= :fromInclusive
              and r.reservationDate <= :toInclusive
              and r.status in :statuses
            """)
    List<Reservation> findBlockingReservationsForCalendar(
            @Param("resourceIds") List<UUID> resourceIds,
            @Param("fromInclusive") LocalDate fromInclusive,
            @Param("toInclusive") LocalDate toInclusive,
            @Param("statuses") List<ReservationStatus> statuses
    );

    List<Reservation> findByUser_IdAndRecurringGroupIdOrderByReservationDateAsc(UUID userId, UUID recurringGroupId);

    List<Reservation> findByUser_IdAndRecurringGroupIdAndReservationDateGreaterThanEqual(
            UUID userId,
            UUID recurringGroupId,
            LocalDate fromDateInclusive
    );
}
