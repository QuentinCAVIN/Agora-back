package com.agora.dto.response.calendar;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Jour calendrier avec créneaux et éventuelle fermeture")
public record CalendarDayDto(
        @Schema(description = "Date (ISO)", example = "2026-04-10")
        LocalDate date,
        @Schema(description = "Jour entièrement fermé (blackout)")
        boolean isBlackout,
        @Schema(description = "Motif de fermeture si blackout", nullable = true)
        String blackoutReason,
        @Schema(description = "Créneaux par ressource, triés par nom de ressource puis heure de début")
        List<CalendarSlotDto> slots
) {
}
