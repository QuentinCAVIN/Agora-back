package com.agora.repository;

import com.agora.entity.Resource;
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
            predicates.add(cb.isTrue(root.get("isActive")));
            if (type != null) {
                predicates.add(cb.equal(root.get("resourceType"), type));
            }
            if (minCapacity != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("capacity"),
                        minCapacity
                ));
            }
            if (available != null) {
                // TODO ici tu brancheras plus tard sur reservations
                // pour l'instant on ignore ou simule
            }

            // 🔹 date (placeholder)
            if (date != null) {
                // TODO à connecter avec Reservation
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}