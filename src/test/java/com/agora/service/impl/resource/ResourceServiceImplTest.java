package com.agora.service.impl.resource;

import com.agora.config.SecurityUtils;
import com.agora.dto.request.resource.ResourceRequest;
import com.agora.dto.response.resource.TimeSlotDto;
import com.agora.entity.resource.Resource;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.enums.resource.ResourceType;
import com.agora.exception.resource.ResourceNotFountException;
import com.agora.mapper.resource.ResourceMapper;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceRepository;
import com.agora.service.impl.resource.ResourceServiceImpl;
import com.agora.testutil.ResourceTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ResourceServiceImplTest {

    @Mock
    private ResourceRepository repository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceMapper mapper;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ResourceServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(securityUtils.hasAuthority(any(), anyString())).thenReturn(false);
    }

    // =========================================
    // CREATE
    // =========================================
    @Test
    void createResource_shouldSaveAndReturnDto() {

        ResourceRequest request = new ResourceRequest(
                "Salle",
                ResourceType.IMMOBILIER,
                100,
                "desc",
                15000,
                ResourceTestData.meetingRoomImage(),
                List.of("PMR")
        );

        Resource entity = new Resource();
        Resource saved = new Resource();
        saved.setId(UUID.randomUUID());

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(
                new com.agora.dto.response.resource.ResourceDto(
                        saved.getId(), "Salle", ResourceType.IMMOBILIER,
                        100, "desc", 15000,
                        ResourceTestData.meetingRoomImage(),
                        List.of("PMR"), true
                )
        );

        var result = service.createResource(request);

        assertThat(result).isNotNull();
        verify(repository).save(entity);
    }

    // =========================================
    // GET BY ID
    // =========================================
    @Test
    void getResourceById_shouldThrow_whenNotFound() {

        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getResourceById(id))
                .isInstanceOf(ResourceNotFountException.class);
    }

    // =========================================
    // UPDATE
    // =========================================
    @Test
    void updateResource_shouldUpdate() {

        UUID id = UUID.randomUUID();

        Resource existing = new Resource();
        existing.setId(id);

        ResourceRequest request = new ResourceRequest(
                "New",
                ResourceType.IMMOBILIER,
                200,
                "desc",
                10000,
                ResourceTestData.randomImage(),
                List.of()
        );

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toDto(existing)).thenReturn(
                new com.agora.dto.response.resource.ResourceDto(
                        id, "New", ResourceType.IMMOBILIER,
                        200, "desc", 10000,
                        ResourceTestData.randomImage(),
                        List.of(), true
                )
        );

        var result = service.updateResource(id, request);

        assertThat(result).isNotNull();
        verify(mapper).updateEntity(existing, request);
    }

    // =========================================
    // DELETE
    // =========================================
    @Test
    void deleteResource_shouldSoftDelete() {

        UUID id = UUID.randomUUID();

        Resource resource = new Resource();
        resource.setId(id);
        resource.setActive(true);

        when(repository.findById(id)).thenReturn(Optional.of(resource));

        service.deleteResource(id);

        assertThat(resource.isActive()).isFalse();
        verify(repository).save(resource);
    }

    // =========================================
    // GET RESOURCES (pagination)
    // =========================================
    @Test
    void getResources_shouldReturnPagedResponse() {

        Page<Resource> page = new PageImpl<>(List.of(new Resource()));

        when(repository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Resource>>any(),
                any(Pageable.class)
        )).thenReturn(page);
        when(mapper.toDto(any())).thenReturn(
                new com.agora.dto.response.resource.ResourceDto(
                        UUID.randomUUID(), "Salle", ResourceType.IMMOBILIER,
                        100, "desc", 15000,
                        ResourceTestData.equipmentImage(),
                        List.of(), true
                )
        );

        var result = service.getResources(null, null, null, null, null, 0, 10);

        assertThat(result.content()).hasSize(1);
    }

    // =========================================
    // GET SLOTS
    // =========================================
    @Test
    void getSlots_shouldReturnAllAvailableSlots() {
        UUID resourceId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 10);
        Resource resource = new Resource();
        resource.setId(resourceId);

        when(repository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(reservationRepository.existsOverlappingSlot(
                any(UUID.class), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class), anyList()
        )).thenReturn(false);

        List<TimeSlotDto> slots = service.getSlots(resourceId, date);

        assertThat(slots).isNotEmpty();
        assertThat(slots).allMatch(s -> s.isAvailable());
        // 10 créneaux de 8h à 18h (60 min chacun)
        assertThat(slots).hasSize(10);
        assertThat(slots.get(0).slotStart()).isEqualTo("08:00");
        assertThat(slots.get(0).slotEnd()).isEqualTo("09:00");
        assertThat(slots.get(9).slotStart()).isEqualTo("17:00");
        assertThat(slots.get(9).slotEnd()).isEqualTo("18:00");
    }

    @Test
    void getSlots_shouldMarkUnavailableSlotsWhenReservationExists() {
        UUID resourceId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 10);
        Resource resource = new Resource();
        resource.setId(resourceId);

        when(repository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(reservationRepository.existsOverlappingSlot(
                eq(resourceId), eq(date), any(), any(), anyList()
        )).thenReturn(false);
        when(reservationRepository.existsOverlappingSlot(
                eq(resourceId), eq(date), eq(LocalTime.of(14, 0)), eq(LocalTime.of(15, 0)), anyList()
        )).thenReturn(true);

        List<TimeSlotDto> slots = service.getSlots(resourceId, date);

        // Vérifier que le créneau 14:00-15:00 est indisponible
        TimeSlotDto slot14h = slots.stream()
                .filter(s -> s.slotStart().equals("14:00"))
                .findFirst()
                .orElse(null);

        assertThat(slot14h).isNotNull();
        assertThat(slot14h.isAvailable()).isFalse();
    }

    @Test
    void getSlots_shouldThrow404WhenResourceNotFound() {
        UUID resourceId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 10);

        when(repository.findById(resourceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSlots(resourceId, date))
                .isInstanceOf(com.agora.exception.BusinessException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void getSlots_shouldGenerateSlotsWithCorrectGranularity() {
        UUID resourceId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 10);
        Resource resource = new Resource();
        resource.setId(resourceId);

        when(repository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(reservationRepository.existsOverlappingSlot(
                any(UUID.class), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class), anyList()
        )).thenReturn(false);

        List<TimeSlotDto> slots = service.getSlots(resourceId, date);

        // Vérifier que les créneaux sont de 60 min et consécutifs
        for (int i = 0; i < slots.size() - 1; i++) {
            TimeSlotDto current = slots.get(i);
            TimeSlotDto next = slots.get(i + 1);

            // Le début du prochain créneau doit être égal à la fin du créneau actuel
            assertThat(next.slotStart()).isEqualTo(current.slotEnd());
        }

        // Vérifier que le premier créneau commence à 08:00
        assertThat(slots.get(0).slotStart()).isEqualTo("08:00");

        // Vérifier que le dernier créneau se termine à 18:00
        assertThat(slots.get(slots.size() - 1).slotEnd()).isEqualTo("18:00");
    }
}