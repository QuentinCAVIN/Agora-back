package com.agora.dto.response.reservation;

import com.agora.enums.resource.ResourceType;

import java.util.UUID;

public record ReservationResourceDto(
        UUID id,
        String name,
        ResourceType resourceType,
        Integer capacity,
        int depositAmountCents,
        String imageUrl
) {
}
