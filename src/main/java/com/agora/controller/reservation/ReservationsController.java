package com.agora.controller.reservation;

import com.agora.dto.request.reservation.CreateReservationRequestDto;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.reservation.ReservationDetailResponseDto;
import com.agora.dto.response.reservation.ReservationSummaryResponseDto;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.service.reservation.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(
        name = "Réservations",
        description = "Gestion des réservations de ressources (créer, consulter, lister)"
)
public class ReservationsController {

    private final ReservationService reservationService;

    @GetMapping
    @Operation(
            summary = "Lister mes réservations",
            description = "Retourne les réservations de l'utilisateur connecté avec pagination et filtrage optionnel par statut"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservations listées"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    public PagedResponse<ReservationSummaryResponseDto> getMyReservations(
            Authentication authentication,
            @Parameter(description = "Filtre par statut (CONFIRMED, CANCELLED, etc.)")
            @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Page (0 par défaut)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille page (max 100)")
            @RequestParam(defaultValue = "20") int size
    ) {
        return reservationService.getMyReservations(authentication, status, page, size);
    }

    @PostMapping
    @Operation(
            summary = "Créer une réservation",
            description = "Créer une nouvelle réservation pour une ressource"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Réservation créée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (groupe requis)"),
            @ApiResponse(responseCode = "404", description = "Ressource ou groupe introuvable"),
            @ApiResponse(responseCode = "409", description = "Créneau indisponible")
    })
    public ResponseEntity<ReservationDetailResponseDto> createReservation(
            @Valid @RequestBody CreateReservationRequestDto request,
            Authentication authentication
    ) {
        ReservationDetailResponseDto response = reservationService.createReservation(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{reservationId}")
    @Operation(
            summary = "Détail d'une réservation",
            description = "Récupérer les détails d'une réservation (accessible si propriétaire)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation trouvée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Accès interdit (pas propriétaire)"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<ReservationDetailResponseDto> getReservationById(
            @Parameter(description = "ID de la réservation")
            @PathVariable UUID reservationId,
            Authentication authentication
    ) {
        ReservationDetailResponseDto response =
                reservationService.getReservationById(reservationId, authentication);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reservationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Annuler une réservation",
            description = "Annuler une réservation active. L'annulation est logique (statut = CANCELLED), pas une suppression physique."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Réservation annulée"),
            @ApiResponse(responseCode = "400", description = "Réservation déjà annulée ou rejetée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Accès interdit (pas propriétaire)"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public void cancelReservation(
            @Parameter(description = "ID de la réservation")
            @PathVariable UUID reservationId,
            Authentication authentication
    ) {
        reservationService.cancelReservation(reservationId, authentication);
    }
}
