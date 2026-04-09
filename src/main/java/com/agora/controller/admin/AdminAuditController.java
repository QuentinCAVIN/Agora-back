package com.agora.controller.admin;

import com.agora.dto.response.admin.AdminAuditPageResponse;
import com.agora.service.admin.AdminAuditQueryService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/audit")
@Tag(name = "Admin Audit", description = "Journal d'audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminAuditQueryService adminAuditQueryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public AdminAuditPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filtre acteur (email, sous-chaîne ou UUID utilisateur — cahier : adminUserId)")
            @RequestParam(required = false) String adminUserId,
            @Parameter(description = "Cible (email, sous-chaîne ou UUID — cahier : targetUserId)")
            @RequestParam(required = false) String targetUserId,
            @RequestParam(required = false) Boolean impersonationOnly,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Filtrer les entrées dont le détail JSON contient ce reservationId (timeline)")
            @RequestParam(required = false) String reservationId
    ) {
        return adminAuditQueryService.list(
                page,
                size,
                adminUserId,
                targetUserId,
                impersonationOnly,
                dateFrom,
                dateTo,
                reservationId
        );
    }
}
