package com.agora.repository.reservation;

import com.agora.entity.reservation.Reservation;
import com.agora.testsupport.TestBookingRefs;
import com.agora.entity.resource.Resource;
import com.agora.entity.user.User;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.enums.resource.ResourceType;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ReservationRepositoryOverlapTest {

    private static final LocalDate DATE = LocalDate.of(2026, 4, 10);
    private static final List<ReservationStatus> BLOCKING_STATUSES = List.of(
            ReservationStatus.PENDING_VALIDATION,
            ReservationStatus.CONFIRMED,
            ReservationStatus.PENDING_DOCUMENT
    );

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void should_accept_when_no_conflict() {
        Resource resource = persistResource("Salle A");
        User user = persistUser("no-conflict@example.com");
        persistReservation(resource, user, LocalTime.of(10, 0), LocalTime.of(12, 0), ReservationStatus.CONFIRMED);

        boolean overlaps = reservationRepository.existsOverlappingSlot(
                resource.getId(),
                DATE,
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                BLOCKING_STATUSES
        );

        assertThat(overlaps).isFalse();
    }

    @Test
    void should_reject_when_total_overlap() {
        Resource resource = persistResource("Salle B");
        User user = persistUser("total-overlap@example.com");
        persistReservation(resource, user, LocalTime.of(10, 0), LocalTime.of(12, 0), ReservationStatus.CONFIRMED);

        boolean overlaps = reservationRepository.existsOverlappingSlot(
                resource.getId(),
                DATE,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                BLOCKING_STATUSES
        );

        assertThat(overlaps).isTrue();
    }

    @Test
    void should_reject_when_overlap_at_start() {
        Resource resource = persistResource("Salle C");
        User user = persistUser("overlap-start@example.com");
        persistReservation(resource, user, LocalTime.of(10, 0), LocalTime.of(12, 0), ReservationStatus.CONFIRMED);

        boolean overlaps = reservationRepository.existsOverlappingSlot(
                resource.getId(),
                DATE,
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                BLOCKING_STATUSES
        );

        assertThat(overlaps).isTrue();
    }

    @Test
    void should_reject_when_overlap_at_end() {
        Resource resource = persistResource("Salle D");
        User user = persistUser("overlap-end@example.com");
        persistReservation(resource, user, LocalTime.of(10, 0), LocalTime.of(12, 0), ReservationStatus.CONFIRMED);

        boolean overlaps = reservationRepository.existsOverlappingSlot(
                resource.getId(),
                DATE,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                BLOCKING_STATUSES
        );

        assertThat(overlaps).isTrue();
    }

    @Test
    void should_accept_when_adjacent_before_or_after() {
        Resource resource = persistResource("Salle E");
        User user = persistUser("adjacent@example.com");
        persistReservation(resource, user, LocalTime.of(10, 0), LocalTime.of(12, 0), ReservationStatus.CONFIRMED);

        boolean adjacentBefore = reservationRepository.existsOverlappingSlot(
                resource.getId(),
                DATE,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0),
                BLOCKING_STATUSES
        );
        boolean adjacentAfter = reservationRepository.existsOverlappingSlot(
                resource.getId(),
                DATE,
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                BLOCKING_STATUSES
        );

        assertThat(adjacentBefore).isFalse();
        assertThat(adjacentAfter).isFalse();
    }

    @Test
    void should_ignore_non_blocking_statuses() {
        Resource resource = persistResource("Salle F");
        User user = persistUser("status-filter@example.com");
        persistReservation(resource, user, LocalTime.of(10, 0), LocalTime.of(12, 0), ReservationStatus.CANCELLED);

        boolean overlapsWithBlockingOnly = reservationRepository.existsOverlappingSlot(
                resource.getId(),
                DATE,
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                BLOCKING_STATUSES
        );
        boolean overlapsWhenCancelledIncluded = reservationRepository.existsOverlappingSlot(
                resource.getId(),
                DATE,
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                List.of(ReservationStatus.CANCELLED)
        );

        assertThat(overlapsWithBlockingOnly).isFalse();
        assertThat(overlapsWhenCancelledIncluded).isTrue();
    }

    private Reservation persistReservation(
            Resource resource,
            User user,
            LocalTime start,
            LocalTime end,
            ReservationStatus status
    ) {
        Reservation reservation = new Reservation();
        reservation.setResource(resource);
        reservation.setUser(user);
        reservation.setReservationDate(DATE);
        reservation.setSlotStart(start);
        reservation.setSlotEnd(end);
        reservation.setStatus(status);
        reservation.setPurpose("Test overlap");
        reservation.setBookingReference(TestBookingRefs.next());
        entityManager.persist(reservation);
        entityManager.flush();
        return reservation;
    }

    private Resource persistResource(String name) {
        Resource resource = Resource.builder()
                .name(name)
                .resourceType(ResourceType.IMMOBILIER)
                .capacity(50)
                .description("Salle de test")
                .depositAmountCents(1000)
                .active(true)
                .build();
        entityManager.persist(resource);
        entityManager.flush();
        return resource;
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAccountType(AccountType.AUTONOMOUS);
        user.setAccountStatus(AccountStatus.ACTIVE);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }
}
