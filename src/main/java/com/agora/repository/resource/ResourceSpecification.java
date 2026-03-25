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
            predicates.add(cb.isTrue(root.get("active")));
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), ResourceType.valueOf(type)));
            }
            if (minCapacity != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("capacity"),
                        minCapacity
                ));
            }
            if (available != null) {
                // TODO brancher sur réservations
            }
            if (date != null) {
                // TODO connecter avec Reservation
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
