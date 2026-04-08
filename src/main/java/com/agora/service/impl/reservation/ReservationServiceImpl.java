package com.agora.service.impl.reservation;

import com.agora.dto.request.reservation.CreateReservationRequestDto;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.reservation.ReservationDetailResponseDto;
import com.agora.dto.response.reservation.ReservationResourceDto;
import com.agora.dto.response.reservation.ReservationSummaryResponseDto;
import com.agora.entity.group.Group;
import com.agora.entity.reservation.Reservation;
import com.agora.entity.resource.Resource;
import com.agora.entity.user.User;
import com.agora.enums.reservation.DepositStatus;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.exception.auth.AuthRequiredException;
import com.agora.exception.auth.AuthUserNotFoundException;
import com.agora.exception.reservation.ReservationForbiddenNoGroupException;
import com.agora.exception.reservation.SlotUnavailableException;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.group.GroupRepository;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceRepository;
import com.agora.repository.user.UserRepository;
import com.agora.service.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    @Override
    @Transactional
    public ReservationDetailResponseDto createReservation(CreateReservationRequestDto request, Authentication authentication) {
        String email = extractAuthenticatedEmail(authentication);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthUserNotFoundException(email));

        Resource resource = resourceRepository.findById(request.resourceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Ressource introuvable"));

        if (!resource.isActive()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Ressource introuvable");
        }

        Group selectedGroup = resolveGroupIfPresent(request.groupId(), user.getId());
        validateReservationRights(selectedGroup, request.groupId());

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

        Reservation saved = reservationRepository.save(reservation);

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
    @Transactional(readOnly = true)
    public PagedResponse<ReservationSummaryResponseDto> getMyReservations(
            Authentication authentication,
            ReservationStatus status,
            int page,
            int size
    ) {
        String email = extractAuthenticatedEmail(authentication);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthUserNotFoundException(email));

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

    private String extractAuthenticatedEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthRequiredException();
        }

        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            throw new AuthRequiredException();
        }

        return email.trim();
    }

    private ReservationSummaryResponseDto toSummaryResponse(Reservation reservation) {
        int depositFull = (int) Math.round(reservation.getResource().getDepositAmountCents());
        int depositApplied = depositFull;
        return new ReservationSummaryResponseDto(
                reservation.getId(),
                reservation.getResource().getName(),
                reservation.getResource().getResourceType(),
                reservation.getReservationDate(),
                reservation.getSlotStart(),
                reservation.getSlotEnd(),
                reservation.getStatus(),
                DepositStatus.DEPOSIT_PENDING,
                depositApplied,
                depositFull,
                "Aucune remise",
                reservation.getCreatedAt()
        );
    }
}
