package com.agora.service.admin;

import com.agora.config.SecurityUtils;
import com.agora.entity.reservation.Reservation;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.service.impl.audit.AuditService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminExportService {

    private final ReservationRepository reservationRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public byte[] exportReservationsCsv(LocalDate dateFrom, LocalDate dateTo, Authentication authentication) {
        Specification<Reservation> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (dateFrom != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("reservationDate"), dateFrom));
            }
            if (dateTo != null) {
                p.add(cb.lessThanOrEqualTo(root.get("reservationDate"), dateTo));
            }
            return p.isEmpty() ? cb.conjunction() : cb.and(p.toArray(Predicate[]::new));
        };
        List<Reservation> rows = reservationRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "reservationDate"));
        StringBuilder sb = new StringBuilder();
        sb.append("id;resource;date;debut;fin;status;deposit\n");
        for (Reservation r : rows) {
            sb.append(r.getId())
                    .append(';')
                    .append(escape(r.getResource().getName()))
                    .append(';')
                    .append(r.getReservationDate())
                    .append(';')
                    .append(r.getSlotStart())
                    .append(';')
                    .append(r.getSlotEnd())
                    .append(';')
                    .append(r.getStatus())
                    .append(';')
                    .append(r.getDepositStatus())
                    .append('\n');
        }
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        String actor = securityUtils.getAuthenticatedEmail(authentication);
        Map<String, Object> d = new HashMap<>();
        d.put("dateFrom", dateFrom.toString());
        d.put("dateTo", dateTo.toString());
        d.put("rowCount", rows.size());
        d.put("byteLength", data.length);
        auditService.log("ADMIN_EXPORT_RESERVATIONS_CSV", actor, null, d, false);
        return data;
    }

    @Transactional(readOnly = true)
    public byte[] exportPaymentsCsv(LocalDate dateFrom, LocalDate dateTo, Authentication authentication) {
        Specification<Reservation> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (dateFrom != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("reservationDate"), dateFrom));
            }
            if (dateTo != null) {
                p.add(cb.lessThanOrEqualTo(root.get("reservationDate"), dateTo));
            }
            return p.isEmpty() ? cb.conjunction() : cb.and(p.toArray(Predicate[]::new));
        };
        List<Reservation> rows = reservationRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "reservationDate"));
        StringBuilder sb = new StringBuilder();
        sb.append("reservationId;status;amountCents;paymentMode;updatedAt\n");
        for (Reservation r : rows) {
            int amount = (int) Math.round(r.getResource().getDepositAmountCents());
            sb.append(r.getId())
                    .append(';')
                    .append(r.getDepositStatus())
                    .append(';')
                    .append(amount)
                    .append(';')
                    .append(r.getPaymentMode() != null ? r.getPaymentMode() : "")
                    .append(';')
                    .append(r.getDepositUpdatedAt() != null ? r.getDepositUpdatedAt().toString() : "")
                    .append('\n');
        }
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        String actor = securityUtils.getAuthenticatedEmail(authentication);
        Map<String, Object> d = new HashMap<>();
        d.put("dateFrom", dateFrom.toString());
        d.put("dateTo", dateTo.toString());
        d.put("rowCount", rows.size());
        d.put("byteLength", data.length);
        auditService.log("ADMIN_EXPORT_PAYMENTS_CSV", actor, null, d, false);
        return data;
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace(";", ",").replace("\n", " ");
    }
}
