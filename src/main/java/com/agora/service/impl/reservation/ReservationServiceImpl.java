package com.agora.service.impl.reservation;

import com.agora.dto.request.reservation.CreateRecurringReservationRequestDto;
import com.agora.dto.request.reservation.CreateReservationRequestDto;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.reservation.ReservationDetailResponseDto;
import com.agora.dto.response.reservation.ReservationResourceDto;
import com.agora.dto.response.reservation.ReservationSummaryResponseDto;
import com.agora.config.SecurityUtils;
import com.agora.entity.group.Group;
import com.agora.entity.reservation.Reservation;
import com.agora.entity.resource.Resource;
import com.agora.entity.user.User;
import com.agora.enums.reservation.DepositStatus;
import com.agora.enums.reservation.RecurrenceFrequency;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.exception.auth.AuthRequiredException;
import com.agora.exception.auth.AuthUserNotFoundException;
import com.agora.exception.reservation.ReservationForbiddenNoGroupException;
import com.agora.exception.reservation.SlotUnavailableException;
import com.agora.repository.calendar.BlackoutPeriodRepository;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.group.GroupRepository;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceRepository;
import com.agora.repository.user.UserRepository;
import com.agora.service.impl.audit.AuditService;
import com.agora.service.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final List<ReservationStatus> BLOCKING_STATUSES = List.of(
            ReservationStatus.PENDING_VALIDATION,
            ReservationStatus.CONFIRMED,
            ReservationStatus.PENDING_DOCUMENT
    );
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RECURRING_OCCURRENCES = 104;

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final BlackoutPeriodRepository blackoutPeriodRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    @Override
    @Transactional
    public ReservationDetailResponseDto createReservation(CreateReservationRequestDto request, Authentication authentication) {
        return createReservationWithRecurringGroup(request, authentication, null);
    }

    private ReservationDetailResponseDto createReservationWithRecurringGroup(
            CreateReservationRequestDto request,
            Authentication authentication,
            UUID recurringGroupId) {
        // Validate input dates and times
        if (request.slotStart() == null || request.slotEnd() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Les heures de début et de fin sont obligatoires");
        }

        if (request.slotStart().compareTo(request.slotEnd()) >= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "L'heure de début doit être antérieure à l'heure de fin");
        }

        if (request.date() != null && request.date().isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "La date de réservation ne peut pas être dans le passé");
        }

        User user = requireCurrentUser(authentication);

        Resource resource = resourceRepository.findById(request.resourceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Ressource introuvable"));

        if (!resource.isActive()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Ressource introuvable");
        }

        Group selectedGroup = resolveGroupIfPresent(request.groupId(), user.getId());
        validateReservationRights(selectedGroup, request.groupId());

        if (blackoutPeriodRepository.countBlockingForResourceOnDate(resource.getId(), request.date()) > 0) {
            throw new SlotUnavailableException(
                    "La ressource est fermée à cette date (période d'indisponibilité planifiée)."
            );
        }

        boolean overlapping = reservationRepository.existsOverlappingSlot(
                resource.getId(),
                request.date(),
                request.slotStart(),
                request.slotEnd(),
                BLOCKING_STATUSES
        );
        if (overlapping) {
            throw new SlotUnavailableException(
                    String.format(
                            "Le créneau %s-%s du %s est déjà réservé.",
                            request.slotStart(), request.slotEnd(), request.date()
                    )
            );
        }

        Reservation reservation = new Reservation();
        reservation.setResource(resource);
        reservation.setUser(user);
        reservation.setReservationDate(request.date());
        reservation.setSlotStart(request.slotStart());
        reservation.setSlotEnd(request.slotEnd());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPurpose(request.purpose());
        reservation.setGroup(selectedGroup);
        reservation.setRecurringGroupId(recurringGroupId);
        reservation.setDepositStatus(DepositStatus.DEPOSIT_PENDING);

        Reservation saved = reservationRepository.save(reservation);

        if (recurringGroupId == null) {
            auditService.log(
                    "RESERVATION_CREATED",
                    auditActor(authentication),
                    null,
                    Map.of(
                            "reservationId", saved.getId().toString(),
                            "resourceId", resource.getId().toString(),
                            "resourceName", resource.getName(),
                            "date", saved.getReservationDate().toString(),
                            "slotStart", saved.getSlotStart().toString(),
                            "slotEnd", saved.getSlotEnd().toString()
                    ),
                    false
            );
        }

        int depositFull = (int) Math.round(resource.getDepositAmountCents());
        int depositApplied = depositFull;
        String discountLabel = "Aucune remise";

        return new ReservationDetailResponseDto(
                saved.getId(),
                resource.getName(),
                resource.getResourceType(),
                saved.getReservationDate(),
                saved.getSlotStart(),
                saved.getSlotEnd(),
                saved.getStatus(),
                DepositStatus.DEPOSIT_PENDING,
                depositApplied,
                depositFull,
                discountLabel,
                saved.getCreatedAt(),
                new ReservationResourceDto(
                        resource.getId(),
                        resource.getName(),
                        resource.getResourceType(),
                        resource.getCapacity(),
                        depositFull,
                        resource.getImageUrl()
                ),
                user.getFirstName() + " " + user.getLastName(),
                selectedGroup != null ? selectedGroup.getName() : null,
                saved.getPurpose(),
                List.of(),
                saved.getRecurringGroupId()
        );
    }

    @Override
    @Transactional
    public List<ReservationSummaryResponseDto> createRecurringReservations(
            CreateRecurringReservationRequestDto request,
            Authentication authentication
    ) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "La date de fin doit être après la date de début.");
        }

        Set<LocalDate> excluded = request.excludedDates() == null
                ? Set.of()
                : new HashSet<>(request.excludedDates());

        List<LocalDate> dates = buildRecurrenceDates(request.startDate(), request.endDate(), request.frequency(), excluded);
        if (dates.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Aucune occurrence de réservation générée.");
        }
        if (dates.size() > MAX_RECURRING_OCCURRENCES) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Trop d'occurrences (" + dates.size() + "). Maximum autorisé : " + MAX_RECURRING_OCCURRENCES
            );
        }

        UUID seriesId = UUID.randomUUID();
        List<ReservationSummaryResponseDto> out = new ArrayList<>();
        for (LocalDate d : dates) {
            CreateReservationRequestDto single = new CreateReservationRequestDto(
                    request.resourceId(),
                    d,
                    request.slotStart(),
                    request.slotEnd(),
                    request.purpose(),
                    request.groupId()
            );
            ReservationDetailResponseDto detail = createReservationWithRecurringGroup(single, authentication, seriesId);
            out.add(toSummaryFromDetail(detail));
        }
        String actorEmail = auditActor(authentication);
        auditService.log(
                "RESERVATION_SERIES_CREATED",
                actorEmail,
                null,
                Map.of(
                        "recurringGroupId", seriesId.toString(),
                        "occurrenceCount", Integer.toString(out.size()),
                        "resourceId", request.resourceId().toString(),
                        "frequency", request.frequency().name(),
                        "startDate", request.startDate().toString(),
                        "endDate", request.endDate().toString()
                ),
                false
        );
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationSummaryResponseDto> listRecurringOccurrences(
            UUID recurringGroupId,
            Authentication authentication
    ) {
        User user = requireCurrentUser(authentication);

        List<Reservation> list = reservationRepository.findByUser_IdAndRecurringGroupIdOrderByReservationDateAsc(
                user.getId(),
                recurringGroupId
        );
        if (list.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Série de réservations introuvable.");
        }
        return list.stream().map(this::toSummaryResponse).toList();
    }

    @Override
    @Transactional
    public void cancelRecurringSeries(UUID recurringGroupId, Authentication authentication) {
        User user = requireCurrentUser(authentication);
        String actorLabel = auditActor(authentication);

        LocalDate today = LocalDate.now();
        List<Reservation> toCancel = reservationRepository
                .findByUser_IdAndRecurringGroupIdAndReservationDateGreaterThanEqual(
                        user.getId(),
                        recurringGroupId,
                        today
                );

        int cancelledCount = 0;
        for (Reservation reservation : toCancel) {
            if (!BLOCKING_STATUSES.contains(reservation.getStatus())) {
                continue;
            }
            if (ReservationStatus.CANCELLED.equals(reservation.getStatus())) {
                continue;
            }
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservation.setCancelledAt(Instant.now());
            reservationRepository.save(reservation);
            cancelledCount++;
        }
        if (cancelledCount > 0) {
            auditService.log(
                    "RESERVATION_SERIES_CANCELLED",
                    actorLabel,
                    null,
                    Map.of(
                            "recurringGroupId", recurringGroupId.toString(),
                            "cancelledOccurrences", Integer.toString(cancelledCount)
                    ),
                    false
            );
        }
    }

    private static List<LocalDate> buildRecurrenceDates(
            LocalDate start,
            LocalDate end,
            RecurrenceFrequency frequency,
            Set<LocalDate> excluded
    ) {
        List<LocalDate> out = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            if (!excluded.contains(cursor)) {
                out.add(cursor);
            }
            cursor = switch (frequency) {
                case WEEKLY -> cursor.plusWeeks(1);
                case BIWEEKLY -> cursor.plusWeeks(2);
                case MONTHLY -> cursor.plusMonths(1);
            };
        }
        return out;
    }

    private ReservationSummaryResponseDto toSummaryFromDetail(ReservationDetailResponseDto d) {
        return new ReservationSummaryResponseDto(
                d.id(),
                d.resourceName(),
                d.resourceType(),
                d.date(),
                d.slotStart(),
                d.slotEnd(),
                d.status(),
                d.depositStatus(),
                d.depositAmountCents(),
                d.depositAmountFullCents(),
                d.discountLabel(),
                d.createdAt(),
                d.userName(),
                d.recurringGroupId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReservationSummaryResponseDto> getMyReservations(
            Authentication authentication,
            ReservationStatus status,
            int page,
            int size
    ) {
        User user = requireCurrentUser(authentication);

        Pageable pageable = PageRequest.of(
                page,
                Math.min(size, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<Reservation> reservationsPage = status == null
                ? reservationRepository.findByUser_Id(user.getId(), pageable)
                : reservationRepository.findByUser_IdAndStatus(user.getId(), status, pageable);

        return PagedResponse.from(reservationsPage.map(this::toSummaryResponse));
    }

    private Group resolveGroupIfPresent(UUID groupId, UUID userId) {
        if (groupId == null) {
            return null;
        }
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Groupe introuvable"));

        boolean member = groupMembershipRepository.existsByUserIdAndGroupId(userId, groupId);
        if (!member) {
            throw new ReservationForbiddenNoGroupException(
                    "Aucun de vos groupes n'autorise la réservation de ressources MOBILIER."
            );
        }
        return group;
    }

    private void validateReservationRights(Group selectedGroup, UUID requestedGroupId) {
        if (requestedGroupId != null && selectedGroup == null) {
            throw new ReservationForbiddenNoGroupException(
                    "Aucun de vos groupes n'autorise la réservation de ressources MOBILIER."
            );
        }

        // Contrat MVP neutre:
        // - groupId null => réservation pour soi (pas de contrôle d'appartenance groupe demandé)
        // - groupId renseigné => contrôle d'appartenance effectué dans resolveGroupIfPresent
    }

    private User requireCurrentUser(Authentication authentication) {
        String subject = extractAuthenticatedSubject(authentication);
        return userRepository.findByJwtSubject(subject)
                .orElseThrow(() -> new AuthUserNotFoundException(subject));
    }

    /**
     * Acteur pour les logs d’audit : admin en impersonation (MDC) sinon email / référence interne de l’utilisateur courant.
     */
    private String auditActor(Authentication authentication) {
        String fromMdc = MDC.get("impersonationAdmin");
        if (fromMdc != null && !fromMdc.isBlank()) {
            return fromMdc;
        }
        User u = requireCurrentUser(authentication);
        if (u.getEmail() != null && !u.getEmail().isBlank()) {
            return u.getEmail().trim();
        }
        return u.getInternalRef() != null ? u.getInternalRef() : u.getId().toString();
    }

    private String extractAuthenticatedSubject(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthRequiredException();
        }

        String subject = authentication.getName();
        if (subject == null || subject.isBlank()) {
            throw new AuthRequiredException();
        }

        return subject.trim();
    }

    private ReservationSummaryResponseDto toSummaryResponse(Reservation reservation) {
        int depositFull = (int) Math.round(reservation.getResource().getDepositAmountCents());
        int depositApplied = depositFull;
        DepositStatus dep = reservation.getDepositStatus() != null
                ? reservation.getDepositStatus()
                : DepositStatus.DEPOSIT_PENDING;
        return new ReservationSummaryResponseDto(
                reservation.getId(),
                reservation.getResource().getName(),
                reservation.getResource().getResourceType(),
                reservation.getReservationDate(),
                reservation.getSlotStart(),
                reservation.getSlotEnd(),
                reservation.getStatus(),
                dep,
                depositApplied,
                depositFull,
                "Aucune remise",
                reservation.getCreatedAt(),
                reservation.getUser().getFirstName() + " " + reservation.getUser().getLastName(),
                reservation.getRecurringGroupId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationDetailResponseDto getReservationById(UUID reservationId, Authentication authentication) {

        User user = requireCurrentUser(authentication);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, // (on pourra améliorer plus tard)
                        "Réservation introuvable"
                ));

        // 3. Vérification ownership (MVP)
        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new BusinessException(
                    ErrorCode.RESERVATION_FORBIDDEN_NO_GROUP, // ⚠️ temporaire (voir remarque)
                    "Accès interdit à cette réservation"
            );
        }

        // 4. Mapping vers DTO (comme createReservation)
        Resource resource = reservation.getResource();
        int depositFull = (int) Math.round(resource.getDepositAmountCents());
        int depositApplied = depositFull;

        return new ReservationDetailResponseDto(
                reservation.getId(),
                resource.getName(),
                resource.getResourceType(),
                reservation.getReservationDate(),
                reservation.getSlotStart(),
                reservation.getSlotEnd(),
                reservation.getStatus(),
                DepositStatus.DEPOSIT_PENDING,
                depositApplied,
                depositFull,
                "Aucune remise",
                reservation.getCreatedAt(),
                new ReservationResourceDto(
                        resource.getId(),
                        resource.getName(),
                        resource.getResourceType(),
                        resource.getCapacity(),
                        depositFull,
                        resource.getImageUrl()
                ),
                reservation.getUser().getFirstName() + " " + reservation.getUser().getLastName(),
                reservation.getGroup() != null ? reservation.getGroup().getName() : null,
                reservation.getPurpose(),
                List.of(),
                reservation.getRecurringGroupId()
        );
    }

    @Override
    @Transactional
    public void cancelReservation(UUID reservationId, Authentication authentication) {
        User user = requireCurrentUser(authentication);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Réservation introuvable"
                ));

        // 3. Vérification ownership
        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new BusinessException(
                    ErrorCode.RESERVATION_FORBIDDEN_NO_GROUP,
                    "Accès interdit à cette réservation"
            );
        }

        // 4. Vérification du statut : éviter double annulation ou annulation de rejet
        if (ReservationStatus.CANCELLED.equals(reservation.getStatus())) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Cette réservation est déjà annulée"
            );
        }

        if (ReservationStatus.REJECTED.equals(reservation.getStatus())) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Impossible d'annuler une réservation rejetée"
            );
        }

        // 5. Passage en CANCELLED et horodatage
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(Instant.now());
        reservationRepository.save(reservation);
    }
}
