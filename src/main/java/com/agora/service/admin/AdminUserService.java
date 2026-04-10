package com.agora.service.admin;

import com.agora.dto.request.admin.ActivateAutonomousRequestDto;
import com.agora.dto.request.admin.CreateTutoredUserRequestDto;
import com.agora.dto.request.admin.UpdateTutoredUserRequestDto;
import com.agora.dto.response.admin.AdminUserDetailResponseDto;
import com.agora.dto.response.admin.AdminUserGroupSnippetDto;
import com.agora.dto.response.admin.AdminUserRowDto;
import com.agora.dto.response.admin.AdminUsersListResponse;
import com.agora.dto.response.admin.ImpersonationTokenResponseDto;
import com.agora.dto.response.auth.GroupDiscountLabelFormatter;
import com.agora.dto.response.auth.UserExemptionLabels;
import com.agora.entity.group.Group;
import com.agora.entity.group.GroupMembership;
import com.agora.entity.user.ERole;
import com.agora.entity.user.User;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.exception.resource.ResourceNotFountException;
import com.agora.repository.auth.AccountActivationTokenRepository;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.group.GroupRepository;
import com.agora.repository.reservation.ReservationDocumentRepository;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.waitlist.WaitlistEntryRepository;
import com.agora.config.SecurityUtils;
import com.agora.repository.user.UserRepository;
import com.agora.service.auth.AccountActivationService;
import com.agora.service.auth.JwtService;
import com.agora.service.impl.audit.AuditService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int MAX_PAGE = 100;

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationDocumentRepository reservationDocumentRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final AccountActivationTokenRepository accountActivationTokenRepository;
    private final AccountActivationService accountActivationService;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    private final AdminUserPrintSummaryService adminUserPrintSummaryService;

    @Transactional(readOnly = true)
    public AdminUsersListResponse listUsers(
            int page,
            int size,
            String accountTypeParam,
            String statusParam,
            String searchQuery
    ) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE);
        Specification<User> spec = buildUserListSpec(accountTypeParam, statusParam, searchQuery);
        Page<User> users = userRepository.findAll(
                spec,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<User> content = users.getContent();
        Map<UUID, List<Group>> groupsByUser = new HashMap<>();
        if (!content.isEmpty()) {
            List<UUID> userIds = content.stream().map(User::getId).toList();
            for (GroupMembership gm : groupMembershipRepository.findAllByUserIdsWithGroup(userIds)) {
                groupsByUser
                        .computeIfAbsent(gm.getUser().getId(), k -> new ArrayList<>())
                        .add(gm.getGroup());
            }
        }
        return new AdminUsersListResponse(
                content.stream()
                        .map(u -> toRow(u, groupsByUser.getOrDefault(u.getId(), List.of())))
                        .toList(),
                users.getTotalElements(),
                users.getTotalPages()
        );
    }

    private Specification<User> buildUserListSpec(
            String accountTypeParam, String statusParam, String searchQuery) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (accountTypeParam != null && !accountTypeParam.isBlank()) {
                preds.add(cb.equal(
                        root.get("accountType"),
                        AccountType.valueOf(accountTypeParam.trim().toUpperCase())
                ));
            }
            if (statusParam != null && !statusParam.isBlank()) {
                AccountStatus st = switch (statusParam.trim().toUpperCase()) {
                    case "ACTIVE" -> AccountStatus.ACTIVE;
                    case "SUSPENDED", "DELETED" -> AccountStatus.DELETED;
                    default -> null;
                };
                if (st != null) {
                    preds.add(cb.equal(root.get("accountStatus"), st));
                }
            }
            if (searchQuery != null && !searchQuery.isBlank()) {
                String frag = "%" + searchQuery.trim().toLowerCase(Locale.ROOT) + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), frag),
                        cb.like(cb.lower(root.get("lastName")), frag),
                        cb.like(cb.lower(root.get("email")), frag)));
            }
            if (preds.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
    }

    @Transactional
    public void suspendUser(UUID userId, Authentication authentication) {
        String actorSubject = securityUtils.getAuthenticatedEmail(authentication);
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFountException("Utilisateur introuvable."));
        if (u.getAccountStatus() == AccountStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Compte déjà suspendu.");
        }
        String targetLabel = u.getEmail() != null && !u.getEmail().isBlank()
                ? u.getEmail()
                : u.getInternalRef();
        u.setAccountStatus(AccountStatus.DELETED);
        userRepository.save(u);
        auditService.log(
                "USER_SUSPENDED",
                actorSubject,
                targetLabel,
                Map.of("userId", userId.toString()),
                false
        );
    }

    @Transactional
    public void reactivateUser(UUID userId, Authentication authentication) {
        String actorSubject = securityUtils.getAuthenticatedEmail(authentication);
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFountException("Utilisateur introuvable."));
        if (u.getAccountStatus() != AccountStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Seuls les comptes suspendus peuvent être réactivés.");
        }
        String targetLabel = u.getEmail() != null && !u.getEmail().isBlank()
                ? u.getEmail()
                : u.getInternalRef();
        u.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(u);
        auditService.log(
                "USER_REACTIVATED",
                actorSubject,
                targetLabel,
                Map.of("userId", userId.toString()),
                false
        );
    }

    /**
     * Suppression physique (purge) : réservations, file d’attente, jetons d’activation, adhésions de groupes.
     * Réservé au secrétariat — impossible pour soi-même ou pour un compte {@link ERole#SUPERADMIN}.
     */
    @Transactional
    public void purgeUser(UUID userId, Authentication authentication) {
        String actorSubject = securityUtils.getAuthenticatedEmail(authentication);
        User actor = userRepository.findByJwtSubject(actorSubject)
                .orElseThrow(() -> new ResourceNotFountException("Utilisateur authentifié introuvable."));
        if (actor.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Impossible de supprimer votre propre compte.");
        }
        User target = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFountException("Utilisateur introuvable."));
        if (target.getRoles() != null && target.getRoles().contains(ERole.SUPERADMIN)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Impossible de supprimer un compte superadmin.");
        }

        String targetLabel = target.getEmail() != null && !target.getEmail().isBlank()
                ? target.getEmail()
                : target.getInternalRef();

        reservationDocumentRepository.deleteByReservation_User_Id(userId);
        reservationRepository.deleteByUser_Id(userId);
        waitlistEntryRepository.deleteByUser_Id(userId);
        accountActivationTokenRepository.deleteByUser_Id(userId);
        groupMembershipRepository.deleteByUser_Id(userId);
        userRepository.delete(target);

        auditService.log(
                "USER_PURGED",
                actorSubject,
                targetLabel,
                Map.of("userId", userId.toString()),
                false
        );
    }

    @Transactional(readOnly = true)
    public ImpersonationTokenResponseDto impersonate(UUID targetUserId, Authentication adminAuth) {
        if (!securityUtils.hasAuthority(adminAuth, "ROLE_SUPERADMIN")
                && !securityUtils.hasAuthority(adminAuth, "ROLE_SECRETARY_ADMIN")
                && !securityUtils.hasAuthority(adminAuth, "ROLE_ADMIN_SUPPORT")) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Réservé au secrétariat / superadmin / support.");
        }
        String adminEmail = securityUtils.getAuthenticatedEmail(adminAuth);
        User target = userRepository.findByIdWithRoles(targetUserId)
                .orElseGet(() -> userRepository.findById(targetUserId)
                        .orElseThrow(() -> new ResourceNotFountException("Utilisateur introuvable.")));
        if (target.getAccountType() != AccountType.TUTORED) {
            throw new BusinessException(ErrorCode.IMPERSONATION_FORBIDDEN);
        }
        if (target.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Le compte cible doit être actif.");
        }
        String accessToken = jwtService.generateImpersonationAccessToken(target, adminEmail);
        String targetLabel = target.getEmail() != null && !target.getEmail().isBlank()
                ? target.getEmail()
                : target.getInternalRef();
        auditService.log(
                "IMPERSONATION_START",
                adminEmail,
                targetLabel,
                Map.of("targetUserId", targetUserId.toString()),
                true
        );
        return new ImpersonationTokenResponseDto(accessToken, jwtService.getImpersonationExpiresInSeconds());
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponseDto getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFountException("Utilisateur introuvable."));
        List<GroupMembership> memberships = groupMembershipRepository.findAllByUserIdWithGroup(user.getId());
        List<Group> memberGroups = memberships.stream().map(GroupMembership::getGroup).toList();
        List<AdminUserGroupSnippetDto> groups = memberships.stream()
                .map(GroupMembership::getGroup)
                .map(this::toAdminGroupSnippet)
                .toList();
        List<String> exemptions = UserExemptionLabels.fromGroups(memberGroups);
        return new AdminUserDetailResponseDto(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAccountType().name(),
                mapAccountStatus(user.getAccountStatus()),
                user.getPhone() != null ? user.getPhone() : "",
                user.getInternalRef(),
                user.getNotesAdmin(),
                groups,
                exemptions,
                user.getCreatedAt().toString()
        );
    }

    private AdminUserGroupSnippetDto toAdminGroupSnippet(Group g) {
        return new AdminUserGroupSnippetDto(
                g.getId().toString(),
                g.getName(),
                GroupDiscountLabelFormatter.format(g.getDiscountType(), g.getDiscountValue()),
                g.isCouncilPowers(),
                g.isCanBookImmobilier(),
                g.isCanBookMobilier()
        );
    }

    @Transactional(readOnly = true)
    public byte[] getUserPrintSummaryPdf(UUID userId) {
        AdminUserDetailResponseDto detail = getUserDetail(userId);
        return adminUserPrintSummaryService.buildPdf(detail);
    }

    @Transactional
    public AdminUserDetailResponseDto createTutored(CreateTutoredUserRequestDto req, Authentication authentication) {
        String actorSubject = securityUtils.getAuthenticatedEmail(authentication);
        Group publicGroup = groupRepository.findByName("Public")
                .orElseThrow(() -> new IllegalStateException("Le groupe preset 'Public' est introuvable"));

        User user = new User();
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setPhone(req.phone());
        user.setNotesAdmin(req.notesAdmin());
        user.setBirthYear(req.birthYear());
        user.setAccountType(AccountType.TUTORED);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setInternalRef(generateUniqueInternalRef(req.lastName(), req.birthYear()));

        User saved = userRepository.save(user);

        GroupMembership membership = new GroupMembership();
        membership.setUser(saved);
        membership.setGroup(publicGroup);
        membership.setJoinedAt(Instant.now());
        groupMembershipRepository.save(membership);

        auditService.log(
                "USER_TUTORED_CREATED",
                actorSubject,
                saved.getInternalRef(),
                Map.of("userId", saved.getId().toString(), "internalRef", saved.getInternalRef()),
                false
        );
        return getUserDetail(saved.getId());
    }

    @Transactional
    public AdminUserDetailResponseDto updateTutored(UUID userId, UpdateTutoredUserRequestDto req, Authentication authentication) {
        String actorSubject = securityUtils.getAuthenticatedEmail(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFountException("Utilisateur introuvable."));
        if (user.getAccountType() != AccountType.TUTORED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Ce compte n'est pas sous tutelle.");
        }
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setPhone(req.phone());
        user.setNotesAdmin(req.notesAdmin());
        user.setBirthYear(req.birthYear());
        userRepository.save(user);
        String targetLabel = user.getEmail() != null && !user.getEmail().isBlank()
                ? user.getEmail()
                : user.getInternalRef();
        auditService.log(
                "USER_TUTORED_UPDATED",
                actorSubject,
                targetLabel,
                Map.of("userId", userId.toString()),
                false
        );
        return getUserDetail(user.getId());
    }

    @Transactional
    public void requestActivateAutonomous(UUID userId, ActivateAutonomousRequestDto req, Authentication authentication) {
        String actorSubject = securityUtils.getAuthenticatedEmail(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFountException("Utilisateur introuvable."));
        if (user.getAccountType() != AccountType.TUTORED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Activation autonome réservée aux comptes tutelle.");
        }
        String email = req.email().trim();
        if (userRepository.findByEmailIgnoreCase(email).filter(u -> !u.getId().equals(user.getId())).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email déjà utilisé.");
        }
        user.setEmail(email);
        userRepository.save(user);
        String token = accountActivationService.issueFreshToken(user);
        log.info("Activation autonome — token émis pour {} (userId={}) : lier /api/auth/activate?token={}",
                email, userId, token);
        auditService.log(
                "USER_ACTIVATION_AUTONOMOUS_REQUESTED",
                actorSubject,
                email,
                Map.of("userId", userId.toString(), "email", email),
                false
        );
    }

    @Transactional
    public void resendActivation(UUID userId, Authentication authentication) {
        String actorSubject = securityUtils.getAuthenticatedEmail(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFountException("Utilisateur introuvable."));
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Aucun email pour renvoyer l'activation.");
        }
        String token = accountActivationService.issueFreshToken(user);
        log.info("Renvoi activation pour {} (userId={}) token={}", user.getEmail(), userId, token);
        auditService.log(
                "USER_ACTIVATION_RESENT",
                actorSubject,
                user.getEmail(),
                Map.of("userId", userId.toString()),
                false
        );
    }

    private String generateUniqueInternalRef(String lastName, Integer birthYear) {
        for (int i = 0; i < 20; i++) {
            String ref = buildInternalRef(lastName, birthYear);
            if (userRepository.findByInternalRef(ref).isEmpty()) {
                return ref;
            }
        }
        throw new IllegalStateException("Impossible de générer une référence interne unique.");
    }

    private static String buildInternalRef(String lastName, Integer birthYear) {
        String base = lastName.replaceAll("[^A-Za-zÀ-ÿ]", "").toUpperCase();
        if (base.length() > 6) {
            base = base.substring(0, 6);
        }
        if (base.isEmpty()) {
            base = "USER";
        }
        String year = birthYear != null ? String.valueOf(birthYear) : "0000";
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return base + "-" + year + "-" + suffix;
    }

    private AdminUserRowDto toRow(User user, List<Group> memberGroups) {
        return new AdminUserRowDto(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAccountType().name(),
                mapAccountStatus(user.getAccountStatus()),
                user.getPhone() != null ? user.getPhone() : "",
                user.getInternalRef(),
                user.getNotesAdmin(),
                UserExemptionLabels.fromGroups(memberGroups),
                user.getCreatedAt().toString()
        );
    }

    private static String mapAccountStatus(AccountStatus status) {
        if (status == null) {
            return "SUSPENDED";
        }
        return switch (status) {
            case ACTIVE -> "ACTIVE";
            case DELETED -> "SUSPENDED";
        };
    }
}
