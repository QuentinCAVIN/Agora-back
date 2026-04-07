package com.agora.service.impl.reservation;

import com.agora.dto.request.reservation.CreateReservationRequestDto;
import com.agora.entity.group.Group;
import com.agora.entity.reservation.Reservation;
import com.agora.entity.resource.Resource;
import com.agora.entity.user.User;
import com.agora.enums.resource.ResourceType;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.exception.reservation.ReservationForbiddenNoGroupException;
import com.agora.exception.reservation.SlotUnavailableException;
import com.agora.repository.group.GroupMembershipRepository;
import com.agora.repository.group.GroupRepository;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceRepository;
import com.agora.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

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
        saved.setStatus(com.agora.enums.reservation.ReservationStatus.CONFIRMED);
        saved.setPurpose("Reunion");
        saved.setCreatedAt(Instant.parse("2026-03-24T11:00:00Z"));

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
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

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
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

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
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
}
