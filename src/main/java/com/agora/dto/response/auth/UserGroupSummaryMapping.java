package com.agora.dto.response.auth;

import com.agora.entity.group.Group;

public final class UserGroupSummaryMapping {

    private UserGroupSummaryMapping() {}

    public static UserGroupSummaryDto fromGroup(Group group, Integer memberCount) {
        return new UserGroupSummaryDto(
                group.getId(),
                group.getName(),
                group.isPreset(),
                group.isCouncilPowers(),
                group.isCanBookImmobilier(),
                group.isCanBookMobilier(),
                group.getDiscountType(),
                group.getDiscountValue(),
                group.getDiscountAppliesTo(),
                GroupDiscountLabelFormatter.format(group.getDiscountType(), group.getDiscountValue()),
                memberCount
        );
    }
}
