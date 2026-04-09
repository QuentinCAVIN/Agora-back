package com.agora.controller.admin;

import com.agora.service.admin.AdminExportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/exports")
@RequiredArgsConstructor
@Tag(name = "Admin Exports", description = "Exports CSV")
public class AdminExportsController {

    private final AdminExportService adminExportService;

    @GetMapping(value = "/reservations", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<byte[]> exportReservations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Authentication authentication
    ) {
        byte[] data = adminExportService.exportReservationsCsv(dateFrom, dateTo, authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reservations.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }

    @GetMapping(value = "/payments", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<byte[]> exportPayments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Authentication authentication
    ) {
        byte[] data = adminExportService.exportPaymentsCsv(dateFrom, dateTo, authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payments.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }
}
