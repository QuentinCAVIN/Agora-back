package com.agora.config.seed;

import com.agora.entity.resource.Resource;
import com.agora.enums.resource.AccessibilityTag;
import com.agora.enums.resource.ResourceType;
import com.agora.repository.resource.ResourceRepository;

import java.util.List;

final class SeedResourcesHelper {

    private final ResourceRepository resourceRepository;

    SeedResourcesHelper(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    void ensureResources() {
        ensureResource(
                "Salle des fêtes — Grande salle",
                ResourceType.IMMOBILIER,
                250,
                "Grande salle pour événements jusqu'à 250 personnes",
                15000,
                "https://mairie-exemple.fr/images/salle-fetes.jpg",
                List.of(AccessibilityTag.PMR_ACCESS, AccessibilityTag.PARKING, AccessibilityTag.SOUND_SYSTEM)
        );

        ensureResource(
                "Salle polyvalente — Petite salle",
                ResourceType.IMMOBILIER,
                80,
                "Salle polyvalente pour réunions et activités associatives",
                8000,
                "https://mairie-exemple.fr/images/salle-polyvalente.jpg",
                List.of(AccessibilityTag.PMR_ACCESS)
        );

        ensureResource(
                "Vidéoprojecteur Epson EB-X51",
                ResourceType.MOBILIER,
                null,
                "Vidéoprojecteur portable avec câble HDMI",
                5000,
                "https://mairie-exemple.fr/images/videoproj.jpg",
                List.of()
        );

        ensureResource(
                "Chaises pliantes (lot de 50)",
                ResourceType.MOBILIER,
                null,
                "Lot de 50 chaises pliantes — retrait en mairie",
                3000,
                "https://mairie-exemple.fr/images/chaises.jpg",
                List.of()
        );
    }

    private void ensureResource(
            String name,
            ResourceType type,
            Integer capacity,
            String description,
            int depositAmountCents,
            String imageUrl,
            List<AccessibilityTag> tags
    ) {
        Resource existing = resourceRepository.findByNameIgnoreCase(name).orElse(null);
        if (existing == null) {
            existing = new Resource();
            existing.setName(name);
        }

        existing.setResourceType(type);
        existing.setCapacity(capacity);
        existing.setDescription(description);
        existing.setDepositAmountCents(depositAmountCents);
        existing.setImageUrl(imageUrl);
        existing.setAccessibilityTags(tags);
        existing.setActive(true);

        resourceRepository.save(existing);
    }
}

