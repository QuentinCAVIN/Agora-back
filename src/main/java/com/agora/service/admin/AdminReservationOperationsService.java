package com.agora.service.admin;

import com.agora.dto.request.admin.AdminPatchReservationStatusRequestDto;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.reservation.AdminReservationListStatsResponseDto;
import com.agora.dto.response.reservation.ReservationSummaryResponseDto;
import com.agora.entity.reservation.Reservation;
import com.agora.entity.user.User;
import com.agora.enums.reservation.DepositStatus;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.exception.resource.ResourceNotFountException;
import com.agora.config.SecurityUtils;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceRepository;
import com.agora.repository.user.UserRepository;
import com.agora.service.group.CouncilMembershipService;
import com.agora.service.impl.audit.AuditService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminReservationOperationsService {

    private static final int MAX_PAGE = 100;

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final CouncilMembershipService councilMembershipService;

    @Transactional(readOnly = true)
    public PagedResponse<ReservationSummaryResponseDto> listReservations(
            List<ReservationStatus> statuses,
            UUID resourceId,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        if (resourceId != null && !resourceRepository.existsById(resourceId)) {
            throw new ResourceNotFountException("Ressource introuvable.");
        }

        Specification<Reservation> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (statuses != null && !statuses.isEmpty()) {
                p.add(root.get("status").in(statuses));
            }
            if (resourceId != null) {
                p.add(cb.equal(root.get("resource").get("id"), resourceId));
            }
            if (dateFrom != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("reservationDate"), dateFrom));
            }
            if (dateTo != null) {
                p.add(cb.lessThanOrEqualTo(root.get("reservationDate"), dateTo));
            }
            return p.isEmpty() ? cb.conjunction() : cb.and(p.toArray(Predicate[]::new));
        };

        Page<Reservation> result = reservationRepository.findAll(spec, pageable);
        return PagedResponse.from(result.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public AdminReservationListStatsResponseDto reservationListStats() {
        long total = reservationRepository.count();
        long pendingGroup = reservationRepository.countByStatusIn(
                List.of(ReservationStatus.PENDING_VALIDATION, ReservationStatus.PENDING_DOCUMENT)
        );
        long confirmed = reservationRepository.countByStatus(ReservationStatus.CONFIRMED);
        long cancelled = reservationRepository.countByStatus(ReservationStatus.CANCELLED);
        long rejected = reservationRepository.countByStatus(ReservationStatus.REJECTED);
        long depositPending = reservationRepository.countByDepositStatus(DepositStatus.DEPOSIT_PENDING);
        long exemptOrWaived = reservationRepository.countByDepositStatusIn(
                Set.of(DepositStatus.EXEMPT, DepositStatus.WAIVED)
        );
        return new AdminReservationListStatsResponseDto(
                total,
                pendingGroup,
                confirmed,
                cancelled,
                rejected,
                depositPending,
                exemptOrWaived
        );
    }

    @Transactional
    public void patchStatus(
            UUID reservationId,
            AdminPatchReservationStatusRequestDto body,
            Authentication authentication
    ) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFountException("Réservation introuvable."));
        ReservationStatus previous = r.getStatus();
        ReservationStatus next = body.status();
        if (next == ReservationStatus.CONFIRMED
                && (previous == ReservationStatus.PENDING_VALIDATION
                        || previous == ReservationStatus.PENDING_DOCUMENT)) {
            assertMayConfirmPendingReservation(authentication);
        }
        String adminEmail = securityUtils.getAuthenticatedEmail(authentication);
        r.setStatus(next);
        if (body.comment() != null && !body.comment().isBlank()) {
            r.setAdminComment(body.comment().trim());
        }
        reservationRepository.save(r);
        User ru = r.getUser();
        String targetLabel = ru.getEmail() != null && !ru.getEmail().isBlank()
                ? ru.getEmail()
                : (ru.getInternalRef() != null ? ru.getInternalRef() : ru.getId().toString());
        auditService.log(
                "RESERVATION_STATUS_ADMIN",
                adminEmail,
                targetLabel,
                Map.of("reservationId", r.getId().toString(), "newStatus", next.name()),
                false
        );
    }

    /**
     * Personnel avec rôle support / délégué : la confirmation nécessite aussi l'appartenance
     * au groupe « conseil municipal » ({@code council_powers}). Secrétaire et superadmin exclus.
     */
    private void assertMayConfirmPendingReservation(Authentication authentication) {
        if (securityUtils.hasAuthority(authentication, "ROLE_SECRETARY_ADMIN")
                || securityUtils.hasAuthority(authentication, "ROLE_SUPERADMIN")) {
            return;
        }
        boolean delegatedStaff = securityUtils.hasAuthority(authentication, "ROLE_ADMIN_SUPPORT")
                || securityUtils.hasAuthority(authentication, "ROLE_DELEGATE_ADMIN");
        if (!delegatedStaff) {
            return;
        }
        User actor = userRepository.findByJwtSubject(authentication.getName().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "Utilisateur introuvable."));
        if (councilMembershipService.isCouncilMember(actor.getId())) {
            return;
        }
        throw new BusinessException(
                ErrorCode.ACCESS_DENIED,
                "La confirmation est réservée au secrétaire ou aux membres du conseil municipal."
        );
    }

    private ReservationSummaryResponseDto toSummary(Reservation reservation) {
        int depositFull = (int) Math.round(reservation.getResource().getDepositAmountCents());
        DepositStatus depositStatus = reservation.getDepositStatus() != null
                ? reservation.getDepositStatus()
                : DepositStatus.DEPOSIT_PENDING;
        int depositApplied = depositFull;
        User u = reservation.getUser();
        String email = u.getEmail() != null && !u.getEmail().isBlank() ? u.getEmail().trim() : null;
        String fn = u.getFirstName() == null ? "" : u.getFirstName();
        String ln = u.getLastName() == null ? "" : u.getLastName();
        String userLabel = (fn + " " + ln).trim();
        if (userLabel.isEmpty()) {
            userLabel = email != null ? email : "—";
        }
        return new ReservationSummaryResponseDto(
                reservation.getId(),
                reservation.getResource().getName(),
                reservation.getResource().getResourceType(),
                reservation.getReservationDate(),
                reservation.getSlotStart(),
                reservation.getSlotEnd(),
                reservation.getStatus(),
                depositStatus,
                depositApplied,
                depositFull,
                "Aucune remise",
                reservation.getCreatedAt(),
                userLabel,
                reservation.getRecurringGroupId(),
                reservation.getBookingReference(),
                email
        );
    }
}
