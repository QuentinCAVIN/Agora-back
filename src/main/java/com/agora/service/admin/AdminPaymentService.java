package com.agora.service.admin;

import com.agora.dto.request.admin.AdminPatchPaymentRequestDto;
import com.agora.dto.response.admin.AdminPaymentHistoryEntryResponseDto;
import com.agora.dto.response.admin.AdminPaymentRowResponseDto;
import com.agora.dto.response.common.PagedResponse;
import com.agora.entity.reservation.DepositPaymentHistory;
import com.agora.entity.reservation.Reservation;
import com.agora.enums.reservation.DepositStatus;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.exception.resource.ResourceNotFountException;
import com.agora.config.SecurityUtils;
import com.agora.repository.reservation.DepositPaymentHistoryRepository;
import com.agora.repository.reservation.ReservationRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private static final int MAX_PAGE = 100;

    private final ReservationRepository reservationRepository;
    private final DepositPaymentHistoryRepository historyRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<AdminPaymentRowResponseDto> listPayments(
            DepositStatus status,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Specification<Reservation> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (status != null) {
                p.add(cb.equal(root.get("depositStatus"), status));
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
        return PagedResponse.from(result.map(this::toRow));
    }

    @Transactional
    public AdminPaymentRowResponseDto patchPayment(
            UUID reservationId,
            AdminPatchPaymentRequestDto body,
            Authentication authentication
    ) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFountException("Réservation introuvable."));
        if (body.status() == DepositStatus.DEPOSIT_PAID && body.paymentMode() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "paymentMode obligatoire si statut DEPOSIT_PAID.");
        }
        DepositStatus previous = r.getDepositStatus() != null ? r.getDepositStatus() : DepositStatus.DEPOSIT_PENDING;
        if (previous == DepositStatus.REFUNDED && body.status() == DepositStatus.DEPOSIT_PAID) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "Transition REFUNDED → DEPOSIT_PAID interdite.");
        }

        String adminLabel = securityUtils.getAuthenticatedEmail(authentication);
        r.setDepositStatus(body.status());
        r.setPaymentMode(body.paymentMode());
        r.setCheckNumber(body.checkNumber());
        r.setPaymentComment(body.comment());
        r.setDepositUpdatedAt(Instant.now());
        r.setDepositUpdatedByName(adminLabel);

        reservationRepository.save(r);

        int amount = body.amountCents() > 0
                ? body.amountCents()
                : (int) Math.round(r.getResource().getDepositAmountCents());

        DepositPaymentHistory h = new DepositPaymentHistory();
        h.setReservation(r);
        h.setStatus(body.status());
        h.setAmountCents(amount);
        h.setPaymentMode(body.paymentMode());
        h.setCheckNumber(body.checkNumber());
        h.setComment(body.comment());
        h.setUpdatedByName(adminLabel);
        historyRepository.save(h);

        var u = r.getUser();
        String targetLabel = u.getEmail() != null && !u.getEmail().isBlank()
                ? u.getEmail()
                : (u.getInternalRef() != null ? u.getInternalRef() : u.getId().toString());
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("reservationId", reservationId.toString());
        auditDetails.put("previousDepositStatus", previous.name());
        auditDetails.put("newDepositStatus", body.status().name());
        auditDetails.put("amountCents", amount);
        if (body.paymentMode() != null) {
            auditDetails.put("paymentMode", body.paymentMode().name());
        }
        auditService.log(
                "DEPOSIT_STATUS_ADMIN",
                adminLabel,
                targetLabel,
                auditDetails,
                false
        );

        return toRow(r);
    }

    @Transactional(readOnly = true)
    public List<AdminPaymentHistoryEntryResponseDto> history(UUID reservationId) {
        if (!reservationRepository.existsById(reservationId)) {
            throw new ResourceNotFountException("Réservation introuvable.");
        }
        return historyRepository.findByReservation_IdOrderByUpdatedAtDesc(reservationId).stream()
                .map(this::toHistoryDto)
                .toList();
    }

    private AdminPaymentRowResponseDto toRow(Reservation r) {
        int amount = (int) Math.round(r.getResource().getDepositAmountCents());
        return new AdminPaymentRowResponseDto(
                r.getId(),
                r.getDepositStatus() != null ? r.getDepositStatus() : DepositStatus.DEPOSIT_PENDING,
                amount,
                r.getPaymentMode(),
                r.getCheckNumber(),
                r.getPaymentComment(),
                r.getDepositUpdatedByName(),
                r.getDepositUpdatedAt()
        );
    }

    private AdminPaymentHistoryEntryResponseDto toHistoryDto(DepositPaymentHistory h) {
        return new AdminPaymentHistoryEntryResponseDto(
                h.getStatus(),
                h.getAmountCents(),
                h.getPaymentMode(),
                h.getCheckNumber(),
                h.getComment(),
                h.getUpdatedByName(),
                h.getUpdatedAt()
        );
    }
}
