package com.agora.dto.response.calendar;

import com.agora.enums.resource.ResourceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Créneau horaire pour une ressource donnée")
public record CalendarSlotDto(
        @Schema(description = "Identifiant de la ressource")
        UUID resourceId,
        @Schema(description = "Nom affiché de la ressource")
        String resourceName,
        @Schema(description = "IMMOBILIER ou MOBILIER")
        ResourceType resourceType,
        @Schema(description = "Heure de début (HH:mm)", example = "08:00")
        String slotStart,
        @Schema(description = "Heure de fin (HH:mm)", example = "10:00")
        String slotEnd,
        @Schema(description = "false si une réservation bloquante occupe le créneau")
        boolean isAvailable
) {
}
