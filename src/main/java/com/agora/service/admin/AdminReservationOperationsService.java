package com.agora.service.admin;

import com.agora.dto.request.admin.AdminPatchReservationStatusRequestDto;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.reservation.ReservationSummaryResponseDto;
import com.agora.entity.reservation.Reservation;
import com.agora.enums.reservation.DepositStatus;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.exception.resource.ResourceNotFountException;
import com.agora.config.SecurityUtils;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceRepository;
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

    @Transactional(readOnly = true)
    public PagedResponse<ReservationSummaryResponseDto> listReservations(
            ReservationStatus status,
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
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
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

    @Transactional
    public void patchStatus(
            UUID reservationId,
            AdminPatchReservationStatusRequestDto body,
            Authentication authentication
    ) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFountException("Réservation introuvable."));
        String adminEmail = securityUtils.getAuthenticatedEmail(authentication);
        r.setStatus(body.status());
        if (body.comment() != null && !body.comment().isBlank()) {
            r.setAdminComment(body.comment().trim());
        }
        reservationRepository.save(r);
        auditService.log(
                "RESERVATION_STATUS_ADMIN",
                adminEmail,
                r.getUser().getEmail(),
                Map.of("reservationId", r.getId().toString(), "newStatus", body.status().name()),
                false
        );
    }

    private ReservationSummaryResponseDto toSummary(Reservation reservation) {
        int depositFull = (int) Math.round(reservation.getResource().getDepositAmountCents());
        DepositStatus depositStatus = reservation.getDepositStatus() != null
                ? reservation.getDepositStatus()
                : DepositStatus.DEPOSIT_PENDING;
        int depositApplied = depositFull;
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
                reservation.getUser().getFirstName() + " " + reservation.getUser().getLastName(),
                reservation.getRecurringGroupId()
        );
    }
}
