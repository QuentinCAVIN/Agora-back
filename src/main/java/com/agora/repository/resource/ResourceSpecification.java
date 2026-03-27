package com.agora.repository.resource;

import com.agora.entity.resource.Resource;
import com.agora.enums.resource.ResourceType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ResourceSpecification {

    public static Specification<Resource> filter(
            String type,
            Integer minCapacity,
            Boolean available,
            LocalDate date
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // ✅ ACTIVE ONLY (contrat catalogue public)
            predicates.add(cb.isTrue(root.get("active")));

            // ✅ TYPE
            if (type != null && !type.isBlank()) {
                ResourceType resourceType = ResourceType.valueOf(type);
                predicates.add(cb.equal(root.get("resourceType"), resourceType));
            }

            // ✅ CAPACITY
            if (minCapacity != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("capacity"),
                        minCapacity
                ));
            }

            // 🔥 TODO PHASE 2 — disponibilité réelle
            if (Boolean.TRUE.equals(available) && date != null) {
                /*
                 * TODO:
                 * - join avec reservations
                 * - exclure créneaux déjà pris
                 * - filtrer par date
                 */
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}