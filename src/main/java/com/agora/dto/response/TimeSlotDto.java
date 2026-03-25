package com.agora.dto.response;

public record TimeSlotDto(
        String slotStart,                    // 'HH:mm'
        String slotEnd,                      // 'HH:mm'
        boolean isAvailable
) {}

