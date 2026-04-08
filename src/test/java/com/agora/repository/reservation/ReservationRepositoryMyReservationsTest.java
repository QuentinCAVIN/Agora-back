package com.agora.repository.reservation;

import com.agora.entity.reservation.Reservation;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ReservationRepositoryMyReservationsTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldReturnOnlyAuthenticatedUserReservations() {
        Resource resource = persistResource("Salle A");
        User userA = persistUser("user-a@example.com");
        User userB = persistUser("user-b@example.com");

        persistReservation(resource, userA, ReservationStatus.CONFIRMED, Instant.parse("2026-03-24T11:00:00Z"));
        persistReservation(resource, userB, ReservationStatus.CONFIRMED, Instant.parse("2026-03-24T12:00:00Z"));

        var page = reservationRepository.findByUser_Id(
                userA.getId(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUser().getId()).isEqualTo(userA.getId());
    }

    @Test
    void shouldFilterByStatusAndKeepStableOrdering() {
        Resource resource = persistResource("Salle B");
        User user = persistUser("user-status@example.com");

        Reservation older = persistReservation(
                resource,
                user,
                ReservationStatus.CONFIRMED,
                Instant.parse("2026-03-24T10:00:00Z")
        );
        Reservation newer = persistReservation(
                resource,
                user,
                ReservationStatus.CONFIRMED,
                Instant.parse("2026-03-24T11:00:00Z")
        );
        persistReservation(
                resource,
                user,
                ReservationStatus.CANCELLED,
                Instant.parse("2026-03-24T12:00:00Z")
        );

        var page = reservationRepository.findByUser_IdAndStatus(
                user.getId(),
                ReservationStatus.CONFIRMED,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Reservation::getId).containsExactly(newer.getId(), older.getId());
    }

    @Test
    void shouldUseIdAsTieBreakerWhenCreatedAtIsEqual() {
        Resource resource = persistResource("Salle C");
        User user = persistUser("user-tiebreak@example.com");
        Instant sameCreatedAt = Instant.parse("2026-03-24T12:00:00Z");

        Reservation first = persistReservation(resource, user, ReservationStatus.CONFIRMED, Instant.now());
        Reservation second = persistReservation(resource, user, ReservationStatus.CONFIRMED, Instant.now());

        entityManager.createNativeQuery("update reservations set created_at = ? where id = ?")
                .setParameter(1, sameCreatedAt)
                .setParameter(2, first.getId())
                .executeUpdate();
        entityManager.createNativeQuery("update reservations set created_at = ? where id = ?")
                .setParameter(1, sameCreatedAt)
                .setParameter(2, second.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        var page = reservationRepository.findByUser_IdAndStatus(
                user.getId(),
                ReservationStatus.CONFIRMED,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );
        var pageSecondCall = reservationRepository.findByUser_IdAndStatus(
                user.getId(),
                ReservationStatus.CONFIRMED,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(pageSecondCall.getContent()).hasSize(2);

        List<Reservation> content = page.getContent();
        List<Reservation> contentSecondCall = pageSecondCall.getContent();

        assertThat(content.get(0).getCreatedAt()).isEqualTo(content.get(1).getCreatedAt());
        assertThat(content).extracting(Reservation::getId).containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(contentSecondCall).extracting(Reservation::getId)
                .containsExactly(content.get(0).getId(), content.get(1).getId());
    }

    private Reservation persistReservation(
            Resource resource,
            User user,
            ReservationStatus status,
            Instant createdAt
    ) {
        Reservation reservation = new Reservation();
        reservation.setResource(resource);
        reservation.setUser(user);
        reservation.setReservationDate(LocalDate.of(2026, 4, 10));
        reservation.setSlotStart(LocalTime.of(14, 0));
        reservation.setSlotEnd(LocalTime.of(18, 0));
        reservation.setStatus(status);
        reservation.setPurpose("Test my reservations");
        reservation.setCreatedAt(createdAt);
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
