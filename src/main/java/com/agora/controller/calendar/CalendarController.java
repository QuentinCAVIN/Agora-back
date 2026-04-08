package com.agora.controller.calendar;

import com.agora.dto.response.calendar.CalendarResponseDto;
import com.agora.service.calendar.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(
        name = "Ressources",
        description = "Catalogue des ressources + calendrier de disponibilité"
)
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/calendar")
    @Operation(
            summary = "Vue calendrier mensuelle",
            description = "Créneaux par ressource pour un mois donné (lecture). Paramètre optionnel resourceId pour filtrer."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Calendrier calculé",
                    content = @Content(schema = @Schema(implementation = CalendarResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides"),
            @ApiResponse(responseCode = "404", description = "Ressource introuvable (resourceId)")
    })
    public CalendarResponseDto getCalendar(
            @Parameter(description = "Année", example = "2026", required = true)
            @RequestParam int year,
            @Parameter(description = "Mois (1-12)", example = "4", required = true)
            @RequestParam int month,
            @Parameter(description = "Filtrer sur une ressource (optionnel)")
            @RequestParam(required = false) UUID resourceId
    ) {
        return calendarService.getMonthlyCalendar(year, month, resourceId);
    }
}
