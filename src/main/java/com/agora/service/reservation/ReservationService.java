package com.agora.service.reservation;

import com.agora.dto.request.reservation.CreateReservationRequestDto;
import com.agora.dto.response.reservation.ReservationDetailResponseDto;
import org.springframework.security.core.Authentication;

public interface ReservationService {

    ReservationDetailResponseDto createReservation(CreateReservationRequestDto request, Authentication authentication);
}
