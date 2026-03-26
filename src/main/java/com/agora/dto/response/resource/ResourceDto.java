package com.agora.dto.response.resource;

import com.agora.enums.resource.ResourceType;

import java.util.List;
import java.util.UUID;

public record ResourceDto(
        UUID id,
        String name,
        ResourceType resourceType,
        Integer capacity,
        String description,
        double depositAmountCents,
        String imageUrl,
        List<String> accessibilityTags,
        boolean isActive
) {
}
