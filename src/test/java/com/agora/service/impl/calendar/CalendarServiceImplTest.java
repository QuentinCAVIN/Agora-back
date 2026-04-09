package com.agora.service.impl.calendar;

import com.agora.dto.response.calendar.CalendarResponseDto;
import com.agora.dto.response.calendar.CalendarSlotDto;
import com.agora.entity.reservation.Reservation;
import com.agora.entity.resource.Resource;
import com.agora.entity.user.User;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.enums.resource.ResourceType;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.repository.calendar.BlackoutPeriodRepository;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CalendarServiceImplTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BlackoutPeriodRepository blackoutPeriodRepository;

    @InjectMocks
    private CalendarServiceImpl calendarService;

    @BeforeEach
    void stubNoBlackouts() {
        lenient().when(blackoutPeriodRepository.findOverlappingRange(any(), any())).thenReturn(List.of());
    }

    @Test
    void getMonthlyCalendar_marksSlotUnavailableWhenReservationOverlaps() {
        UUID resourceId = UUID.randomUUID();
        Resource resource = Resource.builder()
                .id(resourceId)
                .name("Salle A")
                .resourceType(ResourceType.IMMOBILIER)
                .active(true)
                .build();

        LocalDate day = LocalDate.of(2026, 4, 10);

        Reservation reservation = new Reservation();
        reservation.setResource(resource);
        reservation.setReservationDate(day);
        reservation.setSlotStart(LocalTime.of(9, 0));
        reservation.setSlotEnd(LocalTime.of(10, 0));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        User user = new User();
        user.setId(UUID.randomUUID());
        reservation.setUser(user);

        when(resourceRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(resource));
        when(reservationRepository.findBlockingReservationsForCalendar(any(), any(), any(), any()))
                .thenReturn(List.of(reservation));

        CalendarResponseDto response = calendarService.getMonthlyCalendar(2026, 4, null);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(4);
        assertThat(response.days()).hasSize(30);

        List<CalendarSlotDto> slotsForDay = response.days().stream()
                .filter(d -> d.date().equals(day))
                .findFirst()
                .orElseThrow()
                .slots();

        CalendarSlotDto nineToTen = slotsForDay.stream()
                .filter(s -> s.resourceId().equals(resourceId) && "09:00".equals(s.slotStart()))
                .findFirst()
                .orElseThrow();
        assertThat(nineToTen.isAvailable()).isFalse();

        CalendarSlotDto eightToNine = slotsForDay.stream()
                .filter(s -> s.resourceId().equals(resourceId) && "08:00".equals(s.slotStart()))
                .findFirst()
                .orElseThrow();
        assertThat(eightToNine.isAvailable()).isTrue();
    }

    @Test
    void getMonthlyCalendar_unknownResource_throws() {
        UUID missing = UUID.randomUUID();
        when(resourceRepository.findByIdAndActiveTrue(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.getMonthlyCalendar(2026, 4, missing))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getMonthlyCalendar_invalidMonth_throws() {
        assertThatThrownBy(() -> calendarService.getMonthlyCalendar(2026, 13, null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getMonthlyCalendar_yearTooLow_throws() {
        assertThatThrownBy(() -> calendarService.getMonthlyCalendar(1999, 6, null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getMonthlyCalendar_yearTooHigh_throws() {
        assertThatThrownBy(() -> calendarService.getMonthlyCalendar(2101, 1, null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getMonthlyCalendar_monthZero_throws() {
        assertThatThrownBy(() -> calendarService.getMonthlyCalendar(2026, 0, null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getMonthlyCalendar_februaryLeapYear_has29Days() {
        when(resourceRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of());

        CalendarResponseDto response = calendarService.getMonthlyCalendar(2024, 2, null);

        assertThat(response.days()).hasSize(29);
        assertThat(response.days().getFirst().date()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(response.days().getLast().date()).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    void getMonthlyCalendar_noActiveResources_stillReturnsAllDaysWithEmptySlots() {
        when(resourceRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of());

        CalendarResponseDto response = calendarService.getMonthlyCalendar(2026, 4, null);

        assertThat(response.days()).hasSize(30);
        assertThat(response.days()).allMatch(d -> d.slots().isEmpty());
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void getMonthlyCalendar_longReservationBlocksAllDefaultSlots() {
        UUID resourceId = UUID.randomUUID();
        Resource resource = Resource.builder()
                .id(resourceId)
                .name("Grande salle")
                .resourceType(ResourceType.IMMOBILIER)
                .active(true)
                .build();
        LocalDate day = LocalDate.of(2026, 5, 20);

        Reservation longBooking = reservation(resource, day, LocalTime.of(8, 0), LocalTime.of(11, 0));

        when(resourceRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(resource));
        when(reservationRepository.findBlockingReservationsForCalendar(any(), any(), any(), any()))
                .thenReturn(List.of(longBooking));

        CalendarResponseDto response = calendarService.getMonthlyCalendar(2026, 5, null);

        List<CalendarSlotDto> slots = response.days().stream()
                .filter(d -> d.date().equals(day))
                .findFirst()
                .orElseThrow()
                .slots();

        assertThat(slots).hasSize(3);
        assertThat(slots).allMatch(s -> !s.isAvailable());
    }

    @Test
    void getMonthlyCalendar_adjacentReservation_doesNotBlockNextSlot() {
        UUID resourceId = UUID.randomUUID();
        Resource resource = Resource.builder()
                .id(resourceId)
                .name("Salle")
                .resourceType(ResourceType.IMMOBILIER)
                .active(true)
                .build();
        LocalDate day = LocalDate.of(2026, 6, 1);

        Reservation endsAtNine = reservation(resource, day, LocalTime.of(8, 0), LocalTime.of(9, 0));

        when(resourceRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(resource));
        when(reservationRepository.findBlockingReservationsForCalendar(any(), any(), any(), any()))
                .thenReturn(List.of(endsAtNine));

        CalendarResponseDto response = calendarService.getMonthlyCalendar(2026, 6, null);

        List<CalendarSlotDto> slots = daySlots(response, day);
        CalendarSlotDto eightToNine = findSlot(slots, resourceId, "08:00");
        CalendarSlotDto nineToTen = findSlot(slots, resourceId, "09:00");

        assertThat(eightToNine.isAvailable()).isFalse();
        assertThat(nineToTen.isAvailable()).isTrue();
    }

    @Test
    void getMonthlyCalendar_subSlotOverlap_blocksTemplateSlot() {
        UUID resourceId = UUID.randomUUID();
        Resource resource = Resource.builder()
                .id(resourceId)
                .name("Salle")
                .resourceType(ResourceType.IMMOBILIER)
                .active(true)
                .build();
        LocalDate day = LocalDate.of(2026, 6, 15);

        Reservation narrow = reservation(resource, day, LocalTime.of(9, 15), LocalTime.of(9, 45));

        when(resourceRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(resource));
        when(reservationRepository.findBlockingReservationsForCalendar(any(), any(), any(), any()))
                .thenReturn(List.of(narrow));

        CalendarResponseDto response = calendarService.getMonthlyCalendar(2026, 6, null);

        List<CalendarSlotDto> slots = daySlots(response, day);
        assertThat(findSlot(slots, resourceId, "09:00").isAvailable()).isFalse();
        assertThat(findSlot(slots, resourceId, "08:00").isAvailable()).isTrue();
    }

    @Test
    void getMonthlyCalendar_cancelledStatus_notLoadedByRepository_soDoesNotBlock() {
        UUID resourceId = UUID.randomUUID();
        Resource resource = Resource.builder()
                .id(resourceId)
                .name("Salle")
                .resourceType(ResourceType.IMMOBILIER)
                .active(true)
                .build();
        LocalDate day = LocalDate.of(2026, 7, 1);

        when(resourceRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(resource));
        when(reservationRepository.findBlockingReservationsForCalendar(any(), any(), any(), any()))
                .thenReturn(List.of());

        CalendarResponseDto response = calendarService.getMonthlyCalendar(2026, 7, null);

        List<CalendarSlotDto> slots = daySlots(response, day);
        assertThat(slots.stream().filter(s -> s.resourceId().equals(resourceId)))
                .allMatch(CalendarSlotDto::isAvailable);
    }

    @Test
    void getMonthlyCalendar_multipleResources_slotsSortedByResourceName() {
        Resource zebra = Resource.builder()
                .id(UUID.randomUUID())
                .name("Zebra")
                .resourceType(ResourceType.MOBILIER)
                .active(true)
                .build();
        Resource alpha = Resource.builder()
                .id(UUID.randomUUID())
                .name("alpha")
                .resourceType(ResourceType.IMMOBILIER)
                .active(true)
                .build();

        when(resourceRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(zebra, alpha));
        when(reservationRepository.findBlockingReservationsForCalendar(any(), any(), any(), any()))
                .thenReturn(List.of());

        CalendarResponseDto response = calendarService.getMonthlyCalendar(2026, 8, null);

        List<CalendarSlotDto> slots = response.days().getFirst().slots();
        assertThat(slots.getFirst().resourceName()).isEqualTo("alpha");
        assertThat(slots.get(slots.size() - 1).resourceName()).isEqualTo("Zebra");
    }

    @Test
    void getMonthlyCalendar_withResourceId_loadsSingleResource() {
        UUID id = UUID.randomUUID();
        Resource resource = Resource.builder()
                .id(id)
                .name("Une")
                .resourceType(ResourceType.IMMOBILIER)
                .active(true)
                .build();

        when(resourceRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(resource));
        when(reservationRepository.findBlockingReservationsForCalendar(any(), any(), any(), any()))
                .thenReturn(List.of());

        calendarService.getMonthlyCalendar(2026, 9, id);

        verify(resourceRepository).findByIdAndActiveTrue(id);
        verify(resourceRepository, never()).findAllByActiveTrueOrderByNameAsc();
    }

    private static Reservation reservation(Resource resource, LocalDate day, LocalTime start, LocalTime end) {
        Reservation reservation = new Reservation();
        reservation.setResource(resource);
        reservation.setReservationDate(day);
        reservation.setSlotStart(start);
        reservation.setSlotEnd(end);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        User user = new User();
        user.setId(UUID.randomUUID());
        reservation.setUser(user);
        return reservation;
    }

    private static List<CalendarSlotDto> daySlots(CalendarResponseDto response, LocalDate day) {
        return response.days().stream()
                .filter(d -> d.date().equals(day))
                .findFirst()
                .orElseThrow()
                .slots();
    }

    private static CalendarSlotDto findSlot(List<CalendarSlotDto> slots, UUID resourceId, String slotStart) {
        return slots.stream()
                .filter(s -> s.resourceId().equals(resourceId) && slotStart.equals(s.slotStart()))
                .findFirst()
                .orElseThrow();
    }
}
