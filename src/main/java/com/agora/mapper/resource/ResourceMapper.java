package com.agora.mapper.resource;

import com.agora.dto.request.resource.ResourceRequest;
import com.agora.dto.response.resource.ResourceDto;
import com.agora.entity.resource.Resource;
import com.agora.enums.resource.AccessibilityTag;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResourceMapper {

    // =========================
    // DTO
    // =========================
    public ResourceDto toDto(Resource resource) {
        if (resource == null) return null;

        return new ResourceDto(
                resource.getId(),
                resource.getName(),
                resource.getResourceType(),
                resource.getCapacity(),
                resource.getDescription(),
                resource.getDepositAmountCents(),
                resource.getRentalPriceCents(),
                resource.getImageUrl(),
                mapTagsToString(resource.getAccessibilityTags()),
                resource.isActive()
        );
    }

    // =========================
    // ENTITY
    // =========================
    public Resource toEntity(ResourceRequest request) {
        if (request == null) return null;

        return Resource.builder()
                .name(request.name())
                .resourceType(request.resourceType()) // ✅ fix
                .capacity(request.capacity())
                .description(request.description())
                .depositAmountCents(request.depositAmountCents())
                .rentalPriceCents(
                        request.rentalPriceCents() == null
                                ? null
                                : request.rentalPriceCents().doubleValue())
                .imageUrl(request.imageUrl())
                .accessibilityTags(mapTagsToEnum(request.accessibilityTags()))
                .active(true)
                .build();
    }

    // =========================
    // UPDATE
    // =========================
    public void updateEntity(Resource resource, ResourceRequest request) {
        if (resource == null || request == null) return;

        resource.setName(request.name());
        resource.setResourceType(request.resourceType());
        resource.setCapacity(request.capacity());
        resource.setDescription(request.description());
        resource.setDepositAmountCents(request.depositAmountCents());
        resource.setRentalPriceCents(
                request.rentalPriceCents() == null
                        ? null
                        : request.rentalPriceCents().doubleValue());
        resource.setImageUrl(request.imageUrl());
        resource.setAccessibilityTags(mapTagsToEnum(request.accessibilityTags()));

        // 🔒 IMPORTANT
        // on ne touche PAS à active ici
    }

    // =========================
    // MAPPING TAGS
    // =========================
    private List<String> mapTagsToString(List<AccessibilityTag> tags) {
        if (tags == null) return List.of();
        return tags.stream().map(Enum::name).toList();
    }

    private List<AccessibilityTag> mapTagsToEnum(List<String> tags) {
        if (tags == null) return List.of();

        return tags.stream()
                .map(tag -> {
                    try {
                        return AccessibilityTag.valueOf(tag);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Tag invalide: " + tag);
                    }
                })
                .toList();
    }
}