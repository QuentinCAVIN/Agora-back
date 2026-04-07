package com.agora.entity.reservation;

import com.agora.entity.group.Group;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class ReservationJpaTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void should_persist_and_read_valid_reservation() {
        User user = persistValidUser("reservation-jpa-user@example.com");
        Resource resource = persistValidResource("Salle JPA");
        Group group = persistValidGroup("Groupe JPA");

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setReservationDate(LocalDate.of(2026, 4, 10));
        reservation.setSlotStart(LocalTime.of(14, 0));
        reservation.setSlotEnd(LocalTime.of(18, 0));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPurpose("Reunion de travail");
        reservation.setGroup(group);

        entityManager.persist(reservation);
        entityManager.flush();
        entityManager.clear();

        Reservation reloaded = entityManager.find(Reservation.class, reservation.getId());
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getReservationDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(reloaded.getSlotStart()).isEqualTo(LocalTime.of(14, 0));
        assertThat(reloaded.getSlotEnd()).isEqualTo(LocalTime.of(18, 0));
        assertThat(reloaded.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getResource().getId()).isEqualTo(resource.getId());
        assertThat(reloaded.getUser().getId()).isEqualTo(user.getId());
        assertThat(reloaded.getGroup()).isNotNull();
        assertThat(reloaded.getGroup().getId()).isEqualTo(group.getId());

        String persistedStatus = (String) entityManager
                .createNativeQuery("select status from reservations where id = :id")
                .setParameter("id", reservation.getId())
                .getSingleResult();
        assertThat(persistedStatus).isEqualTo("CONFIRMED");
    }

    @Test
    void should_reject_when_resource_is_missing() {
        User user = persistValidUser("reservation-jpa-user-2@example.com");

        Reservation reservation = buildValidReservation();
        reservation.setUser(user);
        reservation.setResource(null);

        assertMandatoryFieldViolation(() -> {
            entityManager.persist(reservation);
            entityManager.flush();
        });
    }

    @Test
    void should_reject_when_user_is_missing() {
        Resource resource = persistValidResource("Salle JPA 2");

        Reservation reservation = buildValidReservation();
        reservation.setUser(null);
        reservation.setResource(resource);

        assertMandatoryFieldViolation(() -> {
            entityManager.persist(reservation);
            entityManager.flush();
        });
    }

    @Test
    void should_reject_when_reservation_date_is_missing() {
        User user = persistValidUser("reservation-jpa-user-3@example.com");
        Resource resource = persistValidResource("Salle JPA 3");

        Reservation missingDate = buildValidReservation();
        missingDate.setUser(user);
        missingDate.setResource(resource);
        missingDate.setReservationDate(null);

        assertMandatoryFieldViolation(() -> {
            entityManager.persist(missingDate);
            entityManager.flush();
        });
    }

    @Test
    void should_reject_when_slot_start_is_missing() {
        User user = persistValidUser("reservation-jpa-user-4@example.com");
        Resource resource = persistValidResource("Salle JPA 4");
        Reservation missingSlotStart = buildValidReservation();
        missingSlotStart.setUser(user);
        missingSlotStart.setResource(resource);
        missingSlotStart.setSlotStart(null);

        assertMandatoryFieldViolation(() -> {
            entityManager.persist(missingSlotStart);
            entityManager.flush();
        });
    }

    @Test
    void should_reject_when_slot_end_is_missing() {
        User user = persistValidUser("reservation-jpa-user-5@example.com");
        Resource resource = persistValidResource("Salle JPA 5");
        Reservation missingSlotEnd = buildValidReservation();
        missingSlotEnd.setUser(user);
        missingSlotEnd.setResource(resource);
        missingSlotEnd.setSlotEnd(null);

        assertMandatoryFieldViolation(() -> {
            entityManager.persist(missingSlotEnd);
            entityManager.flush();
        });
    }

    @Test
    void should_reject_when_status_is_missing() {
        User user = persistValidUser("reservation-jpa-user-6@example.com");
        Resource resource = persistValidResource("Salle JPA 6");
        Reservation missingStatus = buildValidReservation();
        missingStatus.setUser(user);
        missingStatus.setResource(resource);
        missingStatus.setStatus(null);

        assertMandatoryFieldViolation(() -> {
            entityManager.persist(missingStatus);
            entityManager.flush();
        });
    }

    private Reservation buildValidReservation() {
        Reservation reservation = new Reservation();
        reservation.setReservationDate(LocalDate.of(2026, 4, 10));
        reservation.setSlotStart(LocalTime.of(14, 0));
        reservation.setSlotEnd(LocalTime.of(18, 0));
        reservation.setStatus(ReservationStatus.PENDING_VALIDATION);
        reservation.setPurpose("Reservation de test");
        return reservation;
    }

    private User persistValidUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Jean");
        user.setLastName("Dupont");
        user.setPhone("0600000000");
        user.setAccountType(AccountType.AUTONOMOUS);
        user.setAccountStatus(AccountStatus.ACTIVE);
        entityManager.persist(user);
        return user;
    }

    private Resource persistValidResource(String name) {
        Resource resource = new Resource();
        resource.setName(name);
        resource.setDescription("Ressource de test");
        resource.setResourceType(ResourceType.IMMOBILIER);
        resource.setCapacity(120);
        resource.setActive(true);
        resource.setAccessibilityTags(List.of());
        resource.setDepositAmountCents(10000);
        entityManager.persist(resource);
        return resource;
    }

    private Group persistValidGroup(String name) {
        Group group = new Group();
        group.setName(name);
        group.setPreset(false);
        entityManager.persist(group);
        return group;
    }

    private void assertMandatoryFieldViolation(Runnable runnable) {
        assertThatThrownBy(runnable::run)
                .satisfies(this::assertHasConstraintViolationInCauseChain);
    }

    private void assertHasConstraintViolationInCauseChain(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof jakarta.validation.ConstraintViolationException
                    || current instanceof org.hibernate.exception.ConstraintViolationException
                    || current instanceof DataIntegrityViolationException) {
                return;
            }
            current = current.getCause();
        }
        throw new AssertionError("No constraint violation found in exception cause chain", throwable);
    }
}
