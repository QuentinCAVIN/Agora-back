package com.agora.controller.resource;

import com.agora.dto.request.resource.ResourceRequest;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.resource.ResourceDto;
import com.agora.dto.response.resource.TimeSlotDto;
import com.agora.enums.resource.ResourceType;
import com.agora.service.resource.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Tag(
        name = "Ressources",
        description = "Catalogue des ressources (salles, équipements) + disponibilités"
)
public class ResourcesController {

    private final ResourceService resourceService;

    // ======================================================
    // GET LIST (PUBLIC)
    // ======================================================
    @GetMapping
    @Operation(
            summary = "Lister les ressources",
            description = "Retourne une liste paginée des ressources actives. Filtres optionnels."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste récupérée")
    })
    public PagedResponse<ResourceDto> getResources(

            @Parameter(
                    description = "Type de ressource (IMMOBILIER | MOBILIER)",
                    schema = @Schema(allowableValues = {"IMMOBILIER", "MOBILIER"})
            )
            @RequestParam(required = false) String type,

            @Parameter(description = "Capacité minimale")
            @RequestParam(required = false) Integer minCapacity,

            @Parameter(description = "Disponibilité (future implémentation)")
            @RequestParam(required = false) Boolean available,

            @Parameter(description = "Date de disponibilité (YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @Parameter(description = "Page (0 par défaut)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Taille page (max 100)")
            @RequestParam(defaultValue = "20") int size
    ) {
        return resourceService.getResources(type, minCapacity, available, date, page, size);
    }

    // ======================================================
    //  GET BY ID (PUBLIC)
    // ======================================================
    @GetMapping("/{resourceId}")
    @Operation(summary = "Détail d'une ressource")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ressource trouvée"),
            @ApiResponse(responseCode = "404", description = "Ressource introuvable")
    })
    public ResourceDto getResourceById(
            @Parameter(description = "ID de la ressource")
            @PathVariable UUID resourceId
    ) {
        return resourceService.getResourceById(resourceId);
    }

    // ======================================================
    // GET SLOTS (PUBLIC)
    // ======================================================
    @GetMapping("/{resourceId}/slots")
    @Operation(summary = "Créneaux disponibles pour une ressource")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Slots retournés"),
            @ApiResponse(responseCode = "404", description = "Ressource introuvable")
    })
    public List<TimeSlotDto> getSlots(

            @Parameter(description = "ID ressource")
            @PathVariable UUID resourceId,

            @Parameter(description = "Date obligatoire (YYYY-MM-DD)", required = true)
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return resourceService.getSlots(resourceId, date);
    }

    // ======================================================
    //  CREATE (ADMIN)
    // ======================================================
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SECRETARY_ADMIN')")
    @Operation(summary = "Créer une ressource")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ressource créée"),
            @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResourceDto createResource(
            @Valid @RequestBody ResourceRequest request
    ) {
        return resourceService.createResource(request);
    }

    // ======================================================
    //  UPDATE (ADMIN)
    // ======================================================
    @PutMapping("/{resourceId}")
    @PreAuthorize("hasRole('SECRETARY_ADMIN')")
    @Operation(summary = "Modifier une ressource")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ressource mise à jour"),
            @ApiResponse(responseCode = "404", description = "Ressource introuvable")
    })
    public ResourceDto updateResource(
            @PathVariable UUID resourceId,
            @Valid @RequestBody ResourceRequest request
    ) {
        return resourceService.updateResource(resourceId, request);
    }

    // ======================================================
    //  DELETE (ADMIN - SOFT DELETE)
    // ======================================================
    @DeleteMapping("/{resourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SECRETARY_ADMIN')")
    @Operation(summary = "Désactiver une ressource (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ressource désactivée"),
            @ApiResponse(responseCode = "404", description = "Ressource introuvable")
    })
    public void deleteResource(
            @PathVariable UUID resourceId
    ) {
        resourceService.deleteResource(resourceId);
    }
}