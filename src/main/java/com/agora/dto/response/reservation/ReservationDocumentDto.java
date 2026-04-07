package com.agora.dto.response.reservation;

public record ReservationDocumentDto(
        String id,
        String docType,
        String status,
        String uploadedAt
) {
}
