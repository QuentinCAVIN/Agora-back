package com.agora.dto.response.reservation;

import com.agora.enums.reservation.DepositStatus;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.enums.resource.ResourceType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ReservationDetailResponseDto(
        UUID id,
        String resourceName,
        ResourceType resourceType,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm")
        LocalTime slotStart,
        @JsonFormat(pattern = "HH:mm")
        LocalTime slotEnd,
        ReservationStatus status,
        DepositStatus depositStatus,
        int depositAmountCents,
        int depositAmountFullCents,
        String discountLabel,
        Instant createdAt,
        ReservationResourceDto resource,
        String userName,
        String groupName,
        String purpose,
        List<ReservationDocumentDto> documents,
        UUID recurringGroupId
) {
}
