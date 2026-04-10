package com.agora.dto.response.auth;

import com.agora.entity.group.Group;
import com.agora.enums.group.DiscountType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Libellés « exonérations / avantages » alignés sur le profil usager et l’admin : mêmes heuristiques
 * de nom de groupe + {@link GroupDiscountLabelFormatter} pour les remises réelles.
 */
public final class UserExemptionLabels {

    private UserExemptionLabels() {}

    public static List<String> fromGroups(List<Group> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (Group g : groups) {
            if (g.isCouncilPowers()) {
                tags.add("Pouvoir conseil");
            }
            String name = g.getName();
            if (name != null) {
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("association")) {
                    tags.add("Association");
                }
                if (lower.contains("habitant")) {
                    tags.add("Critère social");
                }
            }
        }
        for (Group g : groups) {
            DiscountType dt = g.getDiscountType();
            if (dt != null && dt != DiscountType.NONE) {
                tags.add(GroupDiscountLabelFormatter.format(dt, g.getDiscountValue()));
            }
        }
        return new ArrayList<>(tags);
    }
}
