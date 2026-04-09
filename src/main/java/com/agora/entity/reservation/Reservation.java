package com.agora.entity.reservation;

import com.agora.entity.group.Group;
import com.agora.entity.resource.Resource;
import com.agora.entity.user.User;
import com.agora.enums.reservation.DepositStatus;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.enums.payment.PaymentMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "slot_start", nullable = false)
    private LocalTime slotStart;

    @Column(name = "slot_end", nullable = false)
    private LocalTime slotEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReservationStatus status;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "recurring_group_id")
    private UUID recurringGroupId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_status", nullable = false, length = 50)
    private DepositStatus depositStatus = DepositStatus.DEPOSIT_PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 50)
    private PaymentMode paymentMode;

    @Column(name = "payment_comment", columnDefinition = "TEXT")
    private String paymentComment;

    @Column(name = "check_number", length = 100)
    private String checkNumber;

    @Column(name = "deposit_updated_at")
    private Instant depositUpdatedAt;

    @Column(name = "deposit_updated_by_name", length = 255)
    private String depositUpdatedByName;

    /** Référence métier : {@code yyMMdd} + 5 chiffres (séquence par jour de réservation). */
    @Column(name = "booking_reference", nullable = false, length = 16, unique = true)
    private String bookingReference;
}
