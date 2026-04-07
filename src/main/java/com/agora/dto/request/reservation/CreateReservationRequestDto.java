package com.agora.dto.request.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateReservationRequestDto(
        @NotNull
        UUID resourceId,
        @NotNull
        LocalDate date,
        @NotNull
        LocalTime slotStart,
        @NotNull
        LocalTime slotEnd,
        @NotBlank
        String purpose,
        UUID groupId
) {
}
