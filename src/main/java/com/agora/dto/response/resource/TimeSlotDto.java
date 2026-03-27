package com.agora.dto.response.resource;

public record TimeSlotDto(
        String slotStart,
        String slotEnd,
        boolean isAvailable
) {
}
