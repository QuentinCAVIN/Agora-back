package com.agora.dto.request.resource;

import com.agora.enums.resource.ResourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ResourceRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull ResourceType resourceType,
        @Min(1) Integer capacity,
        String description,
        @Min(0) int depositAmountCents,
        List<String> accessibilityTags
) {
}
