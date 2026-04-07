package com.agora.controller.reservation;

import com.agora.dto.request.reservation.CreateReservationRequestDto;
import com.agora.dto.response.reservation.ReservationDetailResponseDto;
import com.agora.service.reservation.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationsController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationDetailResponseDto> createReservation(
            @Valid @RequestBody CreateReservationRequestDto request,
            Authentication authentication
    ) {
        ReservationDetailResponseDto response = reservationService.createReservation(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
