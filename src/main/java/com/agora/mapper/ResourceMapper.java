package com.agora.mapper;


import com.agora.dto.request.ResourceRequest;
import com.agora.dto.response.ResourceDto;
import com.agora.entity.Resource;
import org.springframework.stereotype.Component;

@Component
public class ResourceMapper {

    // ENTITY → DTO
    public ResourceDto toDto(Resource resource) {
        if (resource == null) return null;

        return new ResourceDto(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getCapacity(),
                resource.getDescription(),
                resource.getDepositAmountCents(),
                resource.getAccessibilityTags(),
                resource.isActive()
        );
    }

    // REQUEST → ENTITY (CREATE)
    public Resource toEntity(ResourceRequest request) {
        if (request == null) return null;

        Resource resource = new Resource();
        resource.setName(request.name());
        resource.setType(request.resourceType());
        resource.setCapacity(request.capacity());
        resource.setDescription(request.description());
        resource.setDepositAmountCents(request.depositAmountCents());
        resource.setAccessibilityTags(request.accessibilityTags());
        resource.setActive(true); // toujours actif à la création

        return resource;
    }

    // UPDATE ENTITY (PATCH / PUT)
    public void updateEntity(Resource resource, ResourceRequest request) {
        if (resource == null || request == null) return;

        resource.setName(request.name());
        resource.setType(request.resourceType());
        resource.setCapacity(request.capacity());
        resource.setDescription(request.description());
        resource.setDepositAmountCents(request.depositAmountCents());
        resource.setAccessibilityTags(request.accessibilityTags());
    }
}