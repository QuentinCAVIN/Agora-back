package com.agora.dto.request.resource;

import com.agora.enums.resource.ResourceType;
import jakarta.validation.constraints.*;

import java.util.List;

public record ResourceRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @NotNull
        ResourceType resourceType,
        Integer capacity,
        @Size(max = 1000)
        String description,
        @Min(0)
        int depositAmountCents,

        /** Null ou absent : tarif catalogue non renseigne. Sinon montant en centimes (0 = gratuit). */
        @Min(0)
        Integer rentalPriceCents,

        @Size(max = 500)
        String imageUrl,
        @NotNull
        List<@NotBlank String>
        accessibilityTags
) {
}