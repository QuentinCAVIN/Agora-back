package com.agora.controller.admin;

import com.agora.dto.request.admin.AdminPatchReservationStatusRequestDto;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.reservation.AdminReservationListStatsResponseDto;
import com.agora.dto.response.reservation.ReservationSummaryResponseDto;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.service.admin.AdminReservationOperationsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
@Tag(name = "Admin Reservations", description = "Validation et suivi des réservations")
public class AdminReservationsController {

    private final AdminReservationOperationsService adminReservationOperationsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public PagedResponse<ReservationSummaryResponseDto> list(
            @RequestParam(required = false) List<ReservationStatus> status,
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminReservationOperationsService.listReservations(status, resourceId, dateFrom, dateTo, page, size);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public AdminReservationListStatsResponseDto stats() {
        return adminReservationOperationsService.reservationListStats();
    }

    @PatchMapping("/{reservationId}/status")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> patchStatus(
            @PathVariable UUID reservationId,
            @Valid @RequestBody AdminPatchReservationStatusRequestDto body,
            Authentication authentication
    ) {
        adminReservationOperationsService.patchStatus(reservationId, body, authentication);
        return ResponseEntity.ok().build();
    }
}
