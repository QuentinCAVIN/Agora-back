package com.agora.service.admin;

import com.agora.dto.response.admin.AdminAuditEntryResponse;
import com.agora.dto.response.admin.AdminAuditPageResponse;
import com.agora.entity.audit.AuditLog;
import com.agora.entity.user.User;
import com.agora.repository.audit.AuditLogRepository;
import com.agora.repository.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuditQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId REPORTING_ZONE = ZoneId.of("Europe/Paris");

    private static final int MAX_TIMELINE_SCAN = 5000;

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminAuditPageResponse list(
            int page,
            int size,
            String adminUserId,
            String targetUserId,
            Boolean impersonationOnly,
            LocalDate dateFrom,
            LocalDate dateTo,
            String reservationId
    ) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "performedAt"));

        String adminFilter = resolveUserFilter(adminUserId);
        String targetFilter = resolveUserFilter(targetUserId);

        if (reservationId != null && !reservationId.isBlank()) {
            // Requête native : ne pas repasser Sort "performedAt" dans le Pageable — Spring le concatène
            // tel quel en SQL (colonne inexistante). L'ORDER BY performed_at est déjà dans la requête.
            Pageable reservationPageable =
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            Page<AuditLog> p = findLogsForReservation(reservationId.trim(), reservationPageable);
            return new AdminAuditPageResponse(
                    p.getContent().stream().map(this::toEntry).toList(),
                    p.getTotalElements(),
                    p.getTotalPages()
            );
        }

        Specification<AuditLog> spec = buildSpecification(adminFilter, targetFilter, impersonationOnly, dateFrom, dateTo);
        Page<AuditLog> p = auditLogRepository.findAll(spec, pageable);

        return new AdminAuditPageResponse(
                p.getContent().stream().map(this::toEntry).toList(),
                p.getTotalElements(),
                p.getTotalPages()
        );
    }

    private Page<AuditLog> findLogsForReservation(String reservationId, Pageable pageable) {
        try {
            return auditLogRepository.findPageByDetailsReservationId(reservationId, pageable);
        } catch (InvalidDataAccessResourceUsageException ex) {
            return filterReservationTimelineInMemory(reservationId, pageable);
        }
    }

    /** Fallback H2 / tests : parcourt un bloc d’entrées récentes (borne {@link #MAX_TIMELINE_SCAN}). */
    private Page<AuditLog> filterReservationTimelineInMemory(String reservationId, Pageable pageable) {
        Pageable scan =
                PageRequest.of(0, MAX_TIMELINE_SCAN, Sort.by(Sort.Direction.DESC, "performedAt"));
        List<AuditLog> all = auditLogRepository.findAll(scan).getContent();
        List<AuditLog> match = all.stream()
                .filter(log -> reservationId.equals(extractReservationIdFromDetails(log)))
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), match.size());
        if (start >= match.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, match.size());
        }
        return new PageImpl<>(match.subList(start, end), pageable, match.size());
    }

    private static String extractReservationIdFromDetails(AuditLog log) {
        if (log.getDetails() == null) {
            return null;
        }
        Object v = log.getDetails().get("reservationId");
        return v != null ? Objects.toString(v, null) : null;
    }

    /**
     * {@code adminUserId} / {@code targetUserId} (cahier) : email complet, sous-chaîne,
     * ou UUID utilisateur résolu vers l’email en base.
     */
    private String resolveUserFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        if (t.contains("@")) {
            return t;
        }
        try {
            UUID id = UUID.fromString(t);
            return userRepository.findById(id).map(User::getEmail).orElse(t);
        } catch (IllegalArgumentException e) {
            return t;
        }
    }

    private Specification<AuditLog> buildSpecification(
            String adminFilter,
            String targetFilter,
            Boolean impersonationOnly,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (adminFilter != null && !adminFilter.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("adminUser")), "%" + adminFilter.toLowerCase() + "%"));
            }
            if (targetFilter != null && !targetFilter.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("targetUser")), "%" + targetFilter.toLowerCase() + "%"));
            }
            if (Boolean.TRUE.equals(impersonationOnly)) {
                predicates.add(cb.isTrue(root.get("impersonation")));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("performedAt"),
                        dateFrom.atStartOfDay(REPORTING_ZONE).toInstant()
                ));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThan(
                        root.get("performedAt"),
                        dateTo.plusDays(1).atStartOfDay(REPORTING_ZONE).toInstant()
                ));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private AdminAuditEntryResponse toEntry(AuditLog log) {
        Map<String, Object> detailsMap = log.getDetails() != null ? log.getDetails() : Collections.emptyMap();
        Map<String, Object> enriched = enrichDetailsForDisplay(detailsMap);
        String target = log.getTargetUser() != null ? log.getTargetUser() : "";
        return new AdminAuditEntryResponse(
                log.getId().toString(),
                log.getAdminUser(),
                target.isBlank() ? null : target,
                log.getAction(),
                enriched,
                log.isImpersonation(),
                log.getPerformedAt()
        );
    }

    private static Map<String, Object> enrichDetailsForDisplay(Map<String, Object> detailsMap) {
        if (detailsMap.isEmpty()) {
            return detailsMap;
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(detailsMap);
        Object rid = detailsMap.get("reservationId");
        if (rid != null) {
            String s = String.valueOf(rid);
            if (s.length() >= 8) {
                out.putIfAbsent("reservationDisplayRef", "Résa · " + s.substring(0, 8));
            }
        }
        return out;
    }
}
