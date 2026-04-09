package com.agora.controller.admin;

import com.agora.dto.request.admin.AdminSupportRequestDto;
import com.agora.dto.response.admin.AdminSupportUserDto;
import com.agora.service.admin.SuperadminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
@Tag(name = "Superadmin", description = "Gestion des comptes ADMIN_SUPPORT")
public class SuperadminController {

    private final SuperadminService superadminService;

    @GetMapping("/admin-support")
    @Operation(summary = "Lister les ADMIN_SUPPORT actifs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "403", description = "Accès réservé au SUPERADMIN")
    })
    public List<AdminSupportUserDto> getAdminSupportUsers() {
        return superadminService.getActiveAdminSupportUsers();
    }

    @PostMapping("/admin-support")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Promouvoir un utilisateur actif en ADMIN_SUPPORT")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utilisateur promu"),
            @ApiResponse(responseCode = "403", description = "Accès réservé au SUPERADMIN"),
            @ApiResponse(responseCode = "409", description = "Utilisateur déjà ADMIN_SUPPORT")
    })
    public AdminSupportUserDto grantAdminSupport(
            @Valid @RequestBody AdminSupportRequestDto request
    ) {
        return superadminService.grantAdminSupport(request.userId());
    }

    @DeleteMapping("/admin-support/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Révoquer le rôle ADMIN_SUPPORT")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rôle révoqué"),
            @ApiResponse(responseCode = "403", description = "Accès réservé au SUPERADMIN")
    })
    public void revokeAdminSupport(@PathVariable UUID userId) {
        superadminService.revokeAdminSupport(userId);
    }

    @DeleteMapping("/secretary-admin/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retirer le rôle SECRETARY_ADMIN (interdit pour le dernier compte actif)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rôle retiré"),
            @ApiResponse(responseCode = "403", description = "Accès réservé au SUPERADMIN"),
            @ApiResponse(responseCode = "409", description = "Dernier secrétaire ou compte inadapté")
    })
    public void revokeSecretaryAdmin(@PathVariable UUID userId) {
        superadminService.revokeSecretaryAdmin(userId);
    }
}
