package com.agora.dto.response.admin;

import com.agora.enums.group.DiscountAppliesTo;
import com.agora.enums.group.DiscountType;

public record AdminGroupResponseDto(
        String id,
        String name,
        boolean isPreset,
        boolean canViewImmobilier,
        boolean canBookImmobilier,
        boolean canViewMobilier,
        boolean canBookMobilier,
        DiscountType discountType,
        int discountValue,
        DiscountAppliesTo discountAppliesTo,
        String discountLabel,
        int memberCount,
        boolean councilPowers
) {
}
