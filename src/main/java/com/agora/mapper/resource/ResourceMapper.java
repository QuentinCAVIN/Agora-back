package com.agora.mapper.resource;

import com.agora.dto.request.resource.ResourceRequest;
import com.agora.dto.response.resource.ResourceDto;
import com.agora.entity.resource.Resource;
import org.springframework.stereotype.Component;

@Component
public class ResourceMapper {

    public ResourceDto toDto(Resource resource) {
        if (resource == null) {
            return null;
        }

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

    public Resource toEntity(ResourceRequest request) {
        if (request == null) {
            return null;
        }

        Resource resource = new Resource();
        resource.setName(request.name());
        resource.setType(request.resourceType());
        resource.setCapacity(request.capacity());
        resource.setDescription(request.description());
        resource.setDepositAmountCents(request.depositAmountCents());
        resource.setAccessibilityTags(request.accessibilityTags());
        resource.setActive(true);

        return resource;
    }

    public void updateEntity(Resource resource, ResourceRequest request) {
        if (resource == null || request == null) {
            return;
        }

        resource.setName(request.name());
        resource.setType(request.resourceType());
        resource.setCapacity(request.capacity());
        resource.setDescription(request.description());
        resource.setDepositAmountCents(request.depositAmountCents());
        resource.setAccessibilityTags(request.accessibilityTags());
    }
}
