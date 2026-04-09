package com.agora.controller.admin;

import com.agora.dto.request.admin.ActivateAutonomousRequestDto;
import com.agora.dto.request.admin.CreateTutoredUserRequestDto;
import com.agora.dto.request.admin.UpdateTutoredUserRequestDto;
import com.agora.dto.response.admin.AdminUserDetailResponseDto;
import com.agora.dto.response.admin.AdminUsersListResponse;
import com.agora.dto.response.admin.ImpersonationTokenResponseDto;
import com.agora.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Utilisateurs côté secrétariat")
public class AdminUsersController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public AdminUsersListResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String status
    ) {
        return adminUserService.listUsers(page, size, accountType, status);
    }

    @GetMapping("/{userId}/print-summary")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Fiche PDF utilisateur (secrétariat)")
    public ResponseEntity<byte[]> printSummary(@PathVariable UUID userId) {
        byte[] pdf = adminUserService.getUserPrintSummaryPdf(userId);
        String filename = "fiche-utilisateur-" + userId + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public AdminUserDetailResponseDto getOne(@PathVariable UUID userId) {
        return adminUserService.getUserDetail(userId);
    }

    @PostMapping("/tutored")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public AdminUserDetailResponseDto createTutored(@Valid @RequestBody CreateTutoredUserRequestDto body) {
        return adminUserService.createTutored(body);
    }

    @PatchMapping("/{userId}/tutored")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public AdminUserDetailResponseDto updateTutored(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateTutoredUserRequestDto body
    ) {
        return adminUserService.updateTutored(userId, body);
    }

    @PostMapping("/{userId}/activate-autonomous")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> activateAutonomous(
            @PathVariable UUID userId,
            @Valid @RequestBody ActivateAutonomousRequestDto body
    ) {
        adminUserService.requestActivateAutonomous(userId, body);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/resend-activation")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> resendActivation(@PathVariable UUID userId) {
        adminUserService.resendActivation(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public void suspend(@PathVariable UUID userId) {
        adminUserService.suspendUser(userId);
    }

    @PostMapping("/{userId}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public void reactivate(@PathVariable UUID userId) {
        adminUserService.reactivateUser(userId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Supprimer définitivement un utilisateur et ses données réservables")
    public void purge(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        adminUserService.purgeUser(userId, authentication);
    }

    @PostMapping("/{userId}/impersonate")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECRETARY_ADMIN', 'ADMIN_SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    public ImpersonationTokenResponseDto impersonate(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        return adminUserService.impersonate(userId, authentication);
    }
}
