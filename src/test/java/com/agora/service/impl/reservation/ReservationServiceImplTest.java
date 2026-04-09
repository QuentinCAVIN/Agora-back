package com.agora.service.impl.reservation;

import com.agora.dto.request.reservation.CreateReservationRequestDto;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.entity.group.Group;
import com.agora.entity.reservation.Reservation;
import com.agora.entity.resource.Resource;
import com.agora.entity.user.User;
import com.agora.enums.resource.ResourceType;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.exception.BusinessException;
import com.agora.exception.auth.AuthRequiredException;
import com.agora.exception.reservation.ReservationForbiddenNoGroupException;
import com.agora.exception.reservation.SlotUnavailableException;
import com.agora.repository.calendar.BlackoutPeriodRepository;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.group.GroupRepository;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceRepository;
import com.agora.repository.user.UserRepository;
import com.agora.config.SecurityUtils;
import com.agora.service.impl.audit.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private BlackoutPeriodRepository blackoutPeriodRepository;
    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @Test
    void getMyReservations_shouldFilterByAuthenticatedUserAndMapSummary() {
        UUID userId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        Resource resource = Resource.builder()
                .id(UUID.randomUUID())
                .name("Salle")
                .resourceType(ResourceType.IMMOBILIER)
                .depositAmountCents(15000)
                .active(true)
                .build();

        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setReservationDate(LocalDate.of(2026, 4, 10));
        reservation.setSlotStart(LocalTime.of(14, 0));
        reservation.setSlotEnd(LocalTime.of(18, 0));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(Instant.parse("2026-03-24T11:00:00Z"));

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findByUser_Id(eq(userId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(reservation), PageRequest.of(0, 20), 1));

        var result = reservationService.getMyReservations(auth, null, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).resourceName()).isEqualTo("Salle");
        assertThat(result.content().get(0).depositAmountFullCents()).isEqualTo(15000);
        assertThat(result.totalElements()).isEqualTo(1);
        verify(reservationRepository).findByUser_Id(eq(userId), any(PageRequest.class));
        verify(reservationRepository, never()).findByUser_IdAndStatus(any(), any(), any(PageRequest.class));
    }

    @Test
    void getMyReservations_shouldApplyStatusFilterWhenProvided() {
        UUID userId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findByUser_IdAndStatus(eq(userId), eq(ReservationStatus.CONFIRMED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var result = reservationService.getMyReservations(auth, ReservationStatus.CONFIRMED, 0, 20);

        assertThat(result.content()).isEmpty();
        verify(reservationRepository).findByUser_IdAndStatus(eq(userId), eq(ReservationStatus.CONFIRMED), any(PageRequest.class));
        verify(reservationRepository, never()).findByUser_Id(any(), any(PageRequest.class));
    }

    @Test
    void getMyReservations_shouldCapPageSizeTo100() {
        UUID userId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findByUser_Id(eq(userId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        var result = reservationService.getMyReservations(auth, null, 0, 500);

        assertThat(result.size()).isEqualTo(100);
        verify(reservationRepository).findByUser_Id(eq(userId), eq(PageRequest.of(
                0,
                100,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
                        .and(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))
        )));
    }

    @Test
    void getMyReservations_shouldFailFastWhenPageOrSizeInvalid() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> reservationService.getMyReservations(auth, null, -1, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reservationService.getMyReservations(auth, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createReservation_shouldReturnResponseWhenValid() {
        UUID resourceId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setFirstName("Jean");
        user.setLastName("Dupont");
        user.setAccountType(AccountType.AUTONOMOUS);
        user.setAccountStatus(AccountStatus.ACTIVE);

        Resource resource = Resource.builder()
                .id(resourceId)
                .name("Salle")
                .resourceType(ResourceType.IMMOBILIER)
                .capacity(100)
                .depositAmountCents(15000)
                .active(true)
                .build();

        Reservation saved = new Reservation();
        saved.setId(UUID.randomUUID());
        saved.setResource(resource);
        saved.setUser(user);
        saved.setReservationDate(LocalDate.of(2026, 4, 10));
        saved.setSlotStart(LocalTime.of(14, 0));
        saved.setSlotEnd(LocalTime.of(18, 0));
        saved.setStatus(ReservationStatus.CONFIRMED);
        saved.setPurpose("Reunion");
        saved.setCreatedAt(Instant.parse("2026-03-24T11:00:00Z"));

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(user));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(reservationRepository.existsOverlappingSlot(any(), any(), any(), any(), anyList())).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

        var response = reservationService.createReservation(
                new CreateReservationRequestDto(
                        resourceId,
                        LocalDate.of(2026, 4, 10),
                        LocalTime.of(14, 0),
                        LocalTime.of(18, 0),
                        "Reunion",
                        null
                ),
                auth
        );

        assertThat(response).isNotNull();
        assertThat(response.status().name()).isEqualTo("CONFIRMED");
    }

    @Test
    void createReservation_shouldThrow403WhenNoGroupForMobiliers() {
        UUID resourceId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setAccountType(AccountType.AUTONOMOUS);
        user.setAccountStatus(AccountStatus.ACTIVE);

        Resource resource = Resource.builder()
                .id(resourceId)
                .name("Salle")
                .resourceType(ResourceType.IMMOBILIER)
                .depositAmountCents(1000)
                .active(true)
                .build();

        Group group = new Group();
        group.setId(groupId);
        group.setName("Association locale");
        group.setPreset(false);

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(user));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMembershipRepository.existsByUserIdAndGroupId(user.getId(), groupId)).thenReturn(false);

        assertThatThrownBy(() -> reservationService.createReservation(
                new CreateReservationRequestDto(
                        resourceId,
                        LocalDate.of(2026, 4, 10),
                        LocalTime.of(14, 0),
                        LocalTime.of(18, 0),
                        "Pret",
                        groupId
                ),
                auth
        )).isInstanceOf(ReservationForbiddenNoGroupException.class);
    }

    @Test
    void createReservation_shouldThrow409WhenSlotUnavailable() {
        UUID resourceId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setAccountType(AccountType.AUTONOMOUS);
        user.setAccountStatus(AccountStatus.ACTIVE);

        Resource resource = Resource.builder()
                .id(resourceId)
                .name("Salle")
                .resourceType(ResourceType.IMMOBILIER)
                .depositAmountCents(15000)
                .active(true)
                .build();

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(user));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(reservationRepository.existsOverlappingSlot(any(), any(), any(), any(), anyList())).thenReturn(true);

        assertThatThrownBy(() -> reservationService.createReservation(
                new CreateReservationRequestDto(
                        resourceId,
                        LocalDate.of(2026, 4, 10),
                        LocalTime.of(14, 0),
                        LocalTime.of(18, 0),
                        "Reunion",
                        null
                ),
                auth
        )).isInstanceOf(SlotUnavailableException.class);
    }

    @Test
    void createReservation_shouldThrow400WhenSlotStartAfterOrEqualSlotEnd() {
        UUID resourceId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // Case: start == end
        assertThatThrownBy(() -> reservationService.createReservation(
                new CreateReservationRequestDto(
                        resourceId,
                        LocalDate.of(2026, 4, 10),
                        LocalTime.of(14, 0),
                        LocalTime.of(14, 0),
                        "Reunion",
                        null
                ),
                auth
        )).isInstanceOf(BusinessException.class);

        // Case: start > end
        assertThatThrownBy(() -> reservationService.createReservation(
                new CreateReservationRequestDto(
                        resourceId,
                        LocalDate.of(2026, 4, 10),
                        LocalTime.of(18, 0),
                        LocalTime.of(14, 0),
                        "Reunion",
                        null
                ),
                auth
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void createReservation_shouldThrow400WhenSlotStartIsNull() {
        UUID resourceId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertThatThrownBy(() -> reservationService.createReservation(
                new CreateReservationRequestDto(
                        resourceId,
                        LocalDate.of(2026, 4, 10),
                        null,
                        LocalTime.of(18, 0),
                        "Reunion",
                        null
                ),
                auth
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void createReservation_shouldThrow400WhenSlotEndIsNull() {
        UUID resourceId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertThatThrownBy(() -> reservationService.createReservation(
                new CreateReservationRequestDto(
                        resourceId,
                        LocalDate.of(2026, 4, 10),
                        LocalTime.of(14, 0),
                        null,
                        "Reunion",
                        null
                ),
                auth
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void createReservation_shouldThrow400WhenReservationDateIsInThePast() {
        UUID resourceId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertThatThrownBy(() -> reservationService.createReservation(
                new CreateReservationRequestDto(
                        resourceId,
                        LocalDate.of(2020, 1, 1),
                        LocalTime.of(14, 0),
                        LocalTime.of(18, 0),
                        "Reunion",
                        null
                ),
                auth
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void getReservationById_shouldReturnDetailWhenOwner() {
        UUID reservationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setFirstName("Jean");
        user.setLastName("Dupont");

        Resource resource = Resource.builder()
                .id(resourceId)
                .name("Salle des fetes")
                .resourceType(ResourceType.IMMOBILIER)
                .capacity(250)
                .depositAmountCents(15000)
                .active(true)
                .imageUrl("https://img")
                .build();

        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setReservationDate(LocalDate.of(2026, 4, 10));
        reservation.setSlotStart(LocalTime.of(14, 0));
        reservation.setSlotEnd(LocalTime.of(18, 0));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPurpose("Reunion associative");
        reservation.setCreatedAt(Instant.parse("2026-03-24T11:00:00Z"));
        reservation.setGroup(null);
        reservation.setRecurringGroupId(null);

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        var response = reservationService.getReservationById(reservationId, auth);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(reservationId);
        assertThat(response.resourceName()).isEqualTo("Salle des fetes");
        assertThat(response.userName()).isEqualTo("Jean Dupont");
        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void getReservationById_shouldThrow404WhenNotFound() {
        UUID reservationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.getReservationById(reservationId, auth))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getReservationById_shouldThrow403WhenNotOwner() {
        UUID reservationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User currentUser = new User();
        currentUser.setId(userId);
        currentUser.setEmail("user@example.com");

        User ownerUser = new User();
        ownerUser.setId(otherUserId);
        ownerUser.setEmail("owner@example.com");

        Resource resource = Resource.builder()
                .id(resourceId)
                .name("Salle")
                .resourceType(ResourceType.IMMOBILIER)
                .depositAmountCents(15000)
                .active(true)
                .build();

        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setUser(ownerUser);
        reservation.setResource(resource);

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(currentUser));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getReservationById(reservationId, auth))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelReservation_shouldUpdateStatusAndSetCancelledAt() {
        UUID reservationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        Resource resource = Resource.builder()
                .id(resourceId)
                .name("Salle")
                .resourceType(ResourceType.IMMOBILIER)
                .depositAmountCents(15000)
                .active(true)
                .build();

        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setReservationDate(LocalDate.of(2026, 4, 10));
        reservation.setSlotStart(LocalTime.of(14, 0));
        reservation.setSlotEnd(LocalTime.of(18, 0));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCancelledAt(null);

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            return saved;
        });

        reservationService.cancelReservation(reservationId, auth);

        verify(reservationRepository).save(any(Reservation.class));
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getCancelledAt()).isNotNull();
    }

    @Test
    void cancelReservation_shouldThrow400WhenAlreadyCancelled() {
        UUID reservationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        Resource resource = Resource.builder()
                .id(resourceId)
                .name("Salle")
                .resourceType(ResourceType.IMMOBILIER)
                .depositAmountCents(15000)
                .active(true)
                .build();

        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(Instant.now());

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(reservationId, auth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà annulée");
    }

    @Test
    void cancelReservation_shouldThrow400WhenRejected() {
        UUID reservationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        Resource resource = Resource.builder()
                .id(resourceId)
                .name("Salle")
                .resourceType(ResourceType.IMMOBILIER)
                .depositAmountCents(15000)
                .active(true)
                .build();

        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStatus(ReservationStatus.REJECTED);

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(reservationId, auth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rejetée");
    }

    @Test
    void cancelReservation_shouldThrow403WhenNotOwner() {
        UUID reservationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User currentUser = new User();
        currentUser.setId(userId);
        currentUser.setEmail("user@example.com");

        User ownerUser = new User();
        ownerUser.setId(otherUserId);
        ownerUser.setEmail("owner@example.com");

        Resource resource = Resource.builder()
                .id(UUID.randomUUID())
                .name("Salle")
                .resourceType(ResourceType.IMMOBILIER)
                .depositAmountCents(15000)
                .active(true)
                .build();

        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setUser(ownerUser);
        reservation.setResource(resource);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(currentUser));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(reservationId, auth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Accès interdit");
    }

    @Test
    void cancelReservation_shouldThrow404WhenNotFound() {
        UUID reservationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        when(userRepository.findByJwtSubject("user@example.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.cancelReservation(reservationId, auth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("introuvable");
    }
}
