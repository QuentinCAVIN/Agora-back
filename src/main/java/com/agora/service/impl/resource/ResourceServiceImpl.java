package com.agora.service.impl.resource;

import com.agora.dto.request.resource.ResourceRequest;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.resource.ResourceDto;
import com.agora.dto.response.resource.TimeSlotDto;
import com.agora.entity.resource.Resource;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.mapper.resource.ResourceMapper;
import com.agora.repository.resource.ResourceRepository;
import com.agora.service.resource.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
                Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

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
    public List<TimeSlotDto> getSlots(UUID resourceId, LocalDate date) {

        resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Ressource introuvable"
                ));

        return List.of(
                new TimeSlotDto("08:00", "09:00", true),
                new TimeSlotDto("09:00", "10:00", false),
                new TimeSlotDto("10:00", "11:00", true)
        );
    }

    @Override
    public ResourceDto createResource(ResourceRequest request) {

        Resource resource = resourceMapper.toEntity(request);

        Resource saved = resourceRepository.save(resource);

        log.info("Ressource créée id={}", saved.getId());

        return resourceMapper.toDto(saved);
    }

    @Override
    public ResourceDto updateResource(UUID resourceId, ResourceRequest request) {

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Ressource introuvable"
                ));

        resourceMapper.updateEntity(resource, request);

        Resource updated = resourceRepository.save(resource);

        log.info("Ressource mise à jour id={}", updated.getId());

        return resourceMapper.toDto(updated);
    }

    @Override
    public void deleteResource(UUID resourceId) {

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Ressource introuvable"
                ));

        resource.setActive(false);

        resourceRepository.save(resource);

        log.warn("Ressource désactivée id={}", resourceId);
    }
}
