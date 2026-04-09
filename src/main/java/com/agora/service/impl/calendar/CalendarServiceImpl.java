package com.agora.service.impl.calendar;

import com.agora.dto.response.calendar.CalendarDayDto;
import com.agora.dto.response.calendar.CalendarResponseDto;
import com.agora.dto.response.calendar.CalendarSlotDto;
import com.agora.entity.calendar.BlackoutPeriod;
import com.agora.entity.reservation.Reservation;
import com.agora.entity.resource.Resource;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.repository.calendar.BlackoutPeriodRepository;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceRepository;
import com.agora.service.calendar.CalendarService;
import com.agora.service.resource.ResourceSlotTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private static final List<ReservationStatus> BLOCKING_STATUSES = List.of(
            ReservationStatus.PENDING_VALIDATION,
            ReservationStatus.CONFIRMED,
            ReservationStatus.PENDING_DOCUMENT
    );

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final BlackoutPeriodRepository blackoutPeriodRepository;

    @Override
    @Transactional(readOnly = true)
    public CalendarResponseDto getMonthlyCalendar(int year, int month, UUID resourceId) {
        validateYearMonth(year, month);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate first = yearMonth.atDay(1);
        LocalDate last = yearMonth.atEndOfMonth();

        List<Resource> resources = resolveResources(resourceId);
        List<UUID> resourceIds = resources.stream().map(Resource::getId).toList();

        List<Reservation> blockingReservations = resourceIds.isEmpty()
                ? List.of()
                : reservationRepository.findBlockingReservationsForCalendar(
                        resourceIds,
                        first,
                        last,
                        BLOCKING_STATUSES
                );

        List<BlackoutPeriod> blackouts = blackoutPeriodRepository.findOverlappingRange(first, last);

        List<CalendarDayDto> days = new ArrayList<>();
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            days.add(buildDay(date, resources, blockingReservations, blackouts));
        }

        return new CalendarResponseDto(year, month, days);
    }

    private void validateYearMonth(int year, int month) {
        if (year < MIN_YEAR || year > MAX_YEAR || month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Paramètres year/month invalides.");
        }
    }

    private List<Resource> resolveResources(UUID resourceId) {
        if (resourceId == null) {
            return resourceRepository.findAllByActiveTrueOrderByNameAsc();
        }
        Resource resource = resourceRepository.findByIdAndActiveTrue(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Ressource introuvable"));
        return List.of(resource);
    }

    private CalendarDayDto buildDay(
            LocalDate date,
            List<Resource> resources,
            List<Reservation> blockingReservations,
            List<BlackoutPeriod> blackouts
    ) {
        List<CalendarSlotDto> slots = new ArrayList<>();
        for (Resource resource : resources) {
            boolean dayBlackout = isResourceBlackedOutOnDate(resource.getId(), date, blackouts);
            for (ResourceSlotTemplate.FixedSlot fixed : ResourceSlotTemplate.defaultSlots()) {
                boolean available =
                        !dayBlackout
                                && !isSlotBlocked(
                                        resource.getId(),
                                        date,
                                        fixed.start(),
                                        fixed.end(),
                                        blockingReservations
                                );
                slots.add(new CalendarSlotDto(
                        resource.getId(),
                        resource.getName(),
                        resource.getResourceType(),
                        fixed.startLabel(),
                        fixed.endLabel(),
                        available
                ));
            }
        }
        slots.sort(Comparator
                .comparing(CalendarSlotDto::resourceName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(CalendarSlotDto::slotStart));

        String blackoutReason = globalBlackoutReasonForDate(date, blackouts);
        boolean isGlobalBlackout = blackoutReason != null;

        return new CalendarDayDto(date, isGlobalBlackout, blackoutReason, slots);
    }

    /**
     * Jour marqué « blackout » dans l’API lorsqu’une fermeture <strong>globale</strong> couvre la date
     * (affichage front). Les fermetures par ressource se traduisent par des créneaux indisponibles.
     */
    private String globalBlackoutReasonForDate(LocalDate date, List<BlackoutPeriod> blackouts) {
        return blackouts.stream()
                .filter(b -> b.getResource() == null)
                .filter(b -> !date.isBefore(b.getDateFrom()) && !date.isAfter(b.getDateTo()))
                .map(BlackoutPeriod::getReason)
                .findFirst()
                .orElse(null);
    }

    private boolean isResourceBlackedOutOnDate(UUID resourceId, LocalDate date, List<BlackoutPeriod> blackouts) {
        return blackouts.stream().anyMatch(b -> {
            if (date.isBefore(b.getDateFrom()) || date.isAfter(b.getDateTo())) {
                return false;
            }
            if (b.getResource() == null) {
                return true;
            }
            return b.getResource().getId().equals(resourceId);
        });
    }

    private boolean isSlotBlocked(
            UUID resourceId,
            LocalDate date,
            LocalTime slotStart,
            LocalTime slotEnd,
            List<Reservation> blockingReservations
    ) {
        return blockingReservations.stream()
                .filter(r -> r.getResource().getId().equals(resourceId))
                .filter(r -> r.getReservationDate().equals(date))
                .anyMatch(r -> slotStart.isBefore(r.getSlotEnd()) && slotEnd.isAfter(r.getSlotStart()));
    }
}
