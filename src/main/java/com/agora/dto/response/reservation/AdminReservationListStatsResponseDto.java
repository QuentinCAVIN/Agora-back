package com.agora.dto.response.reservation;

/**
 * Compteurs globaux pour le tableau de bord admin des réservations (toute la base, hors pagination).
 */
public record AdminReservationListStatsResponseDto(
        long total,
        long pendingGroup,
        long confirmed,
        long cancelled,
        long rejected,
        long depositPending,
        long exemptOrWaived
) {}
