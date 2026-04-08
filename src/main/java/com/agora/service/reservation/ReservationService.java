package com.agora.service.reservation;

import com.agora.dto.request.reservation.CreateReservationRequestDto;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.reservation.ReservationDetailResponseDto;
import com.agora.dto.response.reservation.ReservationSummaryResponseDto;
import com.agora.enums.reservation.ReservationStatus;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface ReservationService {

    ReservationDetailResponseDto createReservation(CreateReservationRequestDto request, Authentication authentication);

    PagedResponse<ReservationSummaryResponseDto> getMyReservations(
            Authentication authentication,
            ReservationStatus status,
            int page,
            int size
    );

    ReservationDetailResponseDto getReservationById(UUID reservationId, Authentication authentication);

    void cancelReservation(UUID reservationId, Authentication authentication);
}
