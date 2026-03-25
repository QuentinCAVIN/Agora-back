package com.agora.controller;


import com.agora.dto.request.ResourceRequest;
import com.agora.dto.response.PagedResponse;
import com.agora.dto.response.ResourceDto;
import com.agora.dto.response.TimeSlotDto;
import com.agora.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
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
        name = "Ressources (Catalogue)",
        description = "Gestion des ressources (salles, équipements) et de leur disponibilité"
)
@SecurityRequirement(name = "Bearer Authentication")
public class ResourcesController {

    private final ResourceService resourceService;

    // ======================================================
    // 📌 LISTE PAGINÉE
    // ======================================================
    @GetMapping
    @Operation(summary = "Liste paginée des ressources")
    public PagedResponse<ResourceDto> getResources(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /resources - type={}, minCapacity={}, available={}, date={}", type, minCapacity, available, date);

        return resourceService.getResources(type, minCapacity, available, date, page, size);
    }

    // ======================================================
    // 📌 DÉTAIL D’UNE RESSOURCE
    // ======================================================
    @GetMapping("/{resourceId}")
    @Operation(summary = "Détail d'une ressource")
    public ResourceDto getResourceById(
            @PathVariable UUID resourceId
    ) {
        log.info("GET /resources/{}", resourceId);

        return resourceService.getResourceById(resourceId);
    }

    // ======================================================
    // 📌 CRÉNEAUX
    // ======================================================
    @GetMapping("/{resourceId}/slots")
    @Operation(summary = "Liste des créneaux disponibles pour une ressource")
    public List<TimeSlotDto> getSlots(
            @PathVariable Long resourceId,
            @Parameter(description = "Date obligatoire (YYYY-MM-DD)", required = true)
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        log.info("GET /resources/{}/slots?date={}", resourceId, date);

        return resourceService.getSlots(resourceId, date);
    }

    // ======================================================
    // 📌 CREATE
    // ======================================================
    @PostMapping
    @PreAuthorize("hasRole('SECRETARY_ADMIN')")
    @Operation(summary = "Créer une ressource")
    public ResourceDto createResource(
            @RequestBody ResourceRequest request
    ) {
        log.info("POST /resources");

        return resourceService.createResource(request);
    }

    // ======================================================
    // 📌 UPDATE
    // ======================================================
    @PutMapping("/{resourceId}")
    @PreAuthorize("hasRole('SECRETARY_ADMIN')")
    @Operation(summary = "Modifier une ressource")
    public ResourceDto updateResource(
            @PathVariable UUID resourceId,
            @RequestBody ResourceRequest request
    ) {
        log.info("PUT /resources/{}", resourceId);

        return resourceService.updateResource(resourceId, request);
    }

    // ======================================================
    // 📌 DELETE (SOFT)
    // ======================================================
    @DeleteMapping("/{resourceId}")
    @PreAuthorize("hasRole('SECRETARY_ADMIN')")
    @Operation(summary = "Désactiver une ressource (soft delete)")
    public void deleteResource(
            @PathVariable UUID resourceId
    ) {
        log.info("DELETE /resources/{}", resourceId);

        resourceService.deleteResource(resourceId);
    }
}