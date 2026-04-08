package com.agora.dto.response.calendar;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Vue calendrier mensuelle (créneaux par ressource, alignée sur le contrat API AGORA)")
public record CalendarResponseDto(
        @Schema(description = "Année", example = "2026")
        int year,
        @Schema(description = "Mois (1-12)", example = "4")
        int month,
        @Schema(description = "Un élément par jour du mois, trié par date")
        List<CalendarDayDto> days
) {
}
