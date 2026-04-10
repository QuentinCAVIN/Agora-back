package com.agora.dto.response.auth;

import com.agora.enums.group.DiscountAppliesTo;
import com.agora.enums.group.DiscountType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserGroupSummaryDto {

    private final UUID id;
    private final String name;
    @JsonProperty("isPreset")
    private final boolean preset;
    /**
     * Membre d'un groupe à pouvoirs conseil (ex. élus) — utilisé pour les exonérations mandat côté réservation.
     */
    private final boolean councilPowers;
    private final boolean canBookImmobilier;
    private final boolean canBookMobilier;
    private final DiscountType discountType;
    private final int discountValue;
    private final DiscountAppliesTo discountAppliesTo;
    private final String discountLabel;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Integer memberCount;
}
