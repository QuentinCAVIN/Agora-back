package com.agora.service.impl.resource;

import com.agora.config.Audited;
import com.agora.config.SecurityUtils;
import com.agora.dto.request.resource.ResourceRequest;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.resource.ResourceDto;
import com.agora.dto.response.resource.TimeSlotDto;
import com.agora.entity.resource.Resource;
import com.agora.enums.resource.ResourceType;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.exception.resource.ResourceImmoCapacityViolationException;
import com.agora.exception.resource.ResourceNotFountException;
import com.agora.mapper.resource.ResourceMapper;
import com.agora.repository.resource.ResourceRepository;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceSpecification;
import com.agora.service.resource.ResourceService;
import com.agora.service.resource.ResourceSlotTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private static final List<ReservationStatus> BLOCKING_STATUSES = List.of(
            ReservationStatus.PENDING_VALIDATION,
            ReservationStatus.CONFIRMED,
            ReservationStatus.PENDING_DOCUMENT
    );
    private static final LocalTime DAY_START = LocalTime.of(8, 0);
    private static final LocalTime DAY_END = LocalTime.of(18, 0);
    private static final int SLOT_DURATION_MINUTES = 60;

    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final ResourceMapper resourceMapper;
    private final SecurityUtils securityUtils;


    @Override
    public PagedResponse<ResourceDto> getResources(
            Authentication authentication,
            String type,
            Integer minCapacity,
            Boolean available,
            LocalDate date,
            int page,
            int size
    ) {
        // Validation en amont (évite les exceptions JPA/DAO "wrappées" -> 500)
        if (type != null && !type.isBlank()) {
            try {
                ResourceType.valueOf(type);
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Type invalide: " + type
                );
            }
        }

        Pageable pageable = PageRequest.of(
                page,
                Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        boolean includeInactive = securityUtils.hasAuthority(authentication, "ROLE_SECRETARY_ADMIN");

        Page<Resource> resources = resourceRepository.findAll(
                ResourceSpecification.filter(includeInactive, type, minCapacity, available, date),
                pageable
        );

        return PagedResponse.from(resources.map(resourceMapper::toDto));
    }


    @Override
    public ResourceDto getResourceById(UUID resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFountException(
                        "Ressource introuvable id=" + resourceId
                ));

        return resourceMapper.toDto(resource);
    }


    @Override
    public List<TimeSlotDto> getSlots(UUID resourceId, LocalDate date) {
        // 1. Vérifier que la ressource existe
        resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Ressource introuvable"
                ));

        // 2. Générer les créneaux de 60 min de 08:00 à 18:00
        List<TimeSlotDto> slots = new ArrayList<>();
        LocalTime currentStart = DAY_START;

        while (currentStart.isBefore(DAY_END)) {
            LocalTime currentEnd = currentStart.plusMinutes(SLOT_DURATION_MINUTES);

            // 3. Vérifier chevauchement avec réservations actives
            boolean isAvailable = !reservationRepository.existsOverlappingSlot(
                    resourceId,
                    date,
                    currentStart,
                    currentEnd,
                    BLOCKING_STATUSES
            );

            // 4. Créer le slot avec le format HH:mm du contrat API
            slots.add(new TimeSlotDto(
                    String.format("%02d:%02d", currentStart.getHour(), currentStart.getMinute()),
                    String.format("%02d:%02d", currentEnd.getHour(), currentEnd.getMinute()),
                    isAvailable
            ));

            currentStart = currentEnd;
        }

        return slots;
    }


    @Override
    @Audited(action = "RESOURCE_CREATED")
    public ResourceDto createResource(ResourceRequest request) {
        validateResource(request);
        Resource resource = resourceMapper.toEntity(request);
        Resource saved = resourceRepository.save(resource);
        log.info("RESOURCE_CREATED id={} type={} capacity={}",
                saved.getId(),
                saved.getResourceType(),
                saved.getCapacity()
        );

        return resourceMapper.toDto(saved);
    }


    @Override
    @Audited(action = "RESOURCE_UPDATED")
    public ResourceDto updateResource(UUID resourceId, ResourceRequest request) {
        validateResource(request);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFountException(
                        "Ressource introuvable id=" + resourceId
                ));

        resourceMapper.updateEntity(resource, request);

        Resource updated = resourceRepository.save(resource);

        log.info("RESOURCE_UPDATED id={}", updated.getId());

        return resourceMapper.toDto(updated);
    }

    @Override
    @Audited(action = "RESOURCE_DEACTIVATED")
    public void deleteResource(UUID resourceId) {

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFountException(
                        "Ressource introuvable id=" + resourceId
                ));

        resource.setActive(false);

        resourceRepository.save(resource);

        log.warn("RESOURCE_DEACTIVATED id={}", resourceId);
    }
    private void validateResource(ResourceRequest request) {

        if (request.resourceType() == ResourceType.IMMOBILIER
                && request.capacity() == null) {

            throw new ResourceImmoCapacityViolationException(
                    "La capacité est obligatoire pour une ressource IMMOBILIER"
            );
        }
    }
}