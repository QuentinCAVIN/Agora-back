package com.agora.service.impl;

import com.agora.dto.request.ResourceRequest;
import com.agora.dto.response.PagedResponse;
import com.agora.dto.response.ResourceDto;
import com.agora.dto.response.TimeSlotDto;
import com.agora.entity.Resource;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.mapper.ResourceMapper;
import com.agora.repository.ResourceRepository;
import com.agora.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;

    // ======================================================
    // 📌 LISTE PAGINÉE
    // ======================================================
    @Override
    public PagedResponse<ResourceDto> getResources(
            String type,
            Integer minCapacity,
            Boolean available,
            LocalDate date,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                Math.min(size, 100), // sécurité max size
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // 👉 pour l’instant simple (tu ajouteras Specification après)
        Page<Resource> resources = resourceRepository.findAll(pageable);

        List<ResourceDto> content = resources
                .map(resourceMapper::toDto)
                .getContent();

        return new PagedResponse<>(
                content,
                resources.getTotalElements(),
                resources.getTotalPages(),
                resources.getNumber(),
                resources.getSize()
        );
    }

    // ======================================================
    // 📌 DETAIL
    // ======================================================
    @Override
    public ResourceDto getResourceById(UUID resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Ressource introuvable avec id=" + resourceId
                ));

        return resourceMapper.toDto(resource);
    }

    @Override
    public List<TimeSlotDto> getSlots(Long resourceId, LocalDate date) {
        return List.of();
    }

    // ======================================================
    // 📌 SLOTS
    // ======================================================
    @Override
    public List<TimeSlotDto> getSlots(UUID resourceId, LocalDate date) {

        resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Ressource introuvable"
                ));

        // 👉 TODO brancher avec Reservation + disponibilité réelle
        return List.of(
                new TimeSlotDto("08:00", "09:00", true),
                new TimeSlotDto("09:00", "10:00", false),
                new TimeSlotDto("10:00", "11:00", true)
        );
    }

    // ======================================================
    // 📌 CREATE
    // ======================================================
    @Override
    public ResourceDto createResource(ResourceRequest request) {

        Resource resource = resourceMapper.toEntity(request);

        Resource saved = resourceRepository.save(resource);

        log.info("✅ Ressource créée id={}", saved.getId());

        return resourceMapper.toDto(saved);
    }


    // ======================================================
    // 📌 UPDATE
    // ======================================================
    @Override
    public ResourceDto updateResource(UUID resourceId, ResourceRequest request) {

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Ressource introuvable"
                ));

        resourceMapper.updateEntity(resource, request);

        Resource updated = resourceRepository.save(resource);

        log.info("✏️ Ressource mise à jour id={}", updated.getId());

        return resourceMapper.toDto(updated);
    }

    // ======================================================
    // 📌 DELETE (SOFT)
    // ======================================================
    @Override
    public void deleteResource(UUID resourceId) {

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Ressource introuvable"
                ));

        resource.setActive(false);

        resourceRepository.save(resource);

        log.warn("🗑️ Ressource désactivée id={}", resourceId);
    }
}