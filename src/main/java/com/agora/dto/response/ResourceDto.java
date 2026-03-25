package com.agora.dto.response;

import com.agora.enums.ResourceType;

import java.util.List;
import java.util.UUID;

public record ResourceDto(
        UUID id,
        String name,
        ResourceType resourceType,           // ROOM | EQUIPMENT
        Integer capacity,                    // null si materiel
        String description,
        double depositAmountCents,              // montant caution en centimes (0 = gratuit)
        List<String> accessibilityTags,      // ['PMR_ACCESS','PARKING','SOUND_SYSTEM'...]
        boolean isActive
) {}

