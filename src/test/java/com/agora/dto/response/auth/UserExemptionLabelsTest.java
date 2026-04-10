package com.agora.dto.response.auth;

import com.agora.entity.group.Group;
import com.agora.enums.group.DiscountAppliesTo;
import com.agora.enums.group.DiscountType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserExemptionLabelsTest {

    @Test
    void fromGroups_councilAndDiscount_collectsLabels() {
        Group council = new Group();
        council.setId(UUID.randomUUID());
        council.setName("Conseillers municipaux");
        council.setCouncilPowers(true);
        council.setDiscountType(DiscountType.FULL_EXEMPT);
        council.setDiscountValue(0);
        council.setDiscountAppliesTo(DiscountAppliesTo.ALL);

        Group pub = new Group();
        pub.setId(UUID.randomUUID());
        pub.setName("Public");
        pub.setCouncilPowers(false);
        pub.setDiscountType(DiscountType.NONE);
        pub.setDiscountValue(0);
        pub.setDiscountAppliesTo(DiscountAppliesTo.ALL);

        List<String> labels = UserExemptionLabels.fromGroups(List.of(council, pub));

        assertTrue(labels.contains("Pouvoir conseil"));
        assertTrue(labels.contains("Exonération totale"));
    }

    @Test
    void fromGroups_associationName_addsAssociationTag() {
        Group assoc = new Group();
        assoc.setId(UUID.randomUUID());
        assoc.setName("Association sportive locale");
        assoc.setDiscountType(DiscountType.PERCENTAGE);
        assoc.setDiscountValue(10);
        assoc.setDiscountAppliesTo(DiscountAppliesTo.ALL);

        List<String> labels = UserExemptionLabels.fromGroups(List.of(assoc));

        assertTrue(labels.contains("Association"));
        assertTrue(labels.stream().anyMatch(s -> s.contains("10")));
    }
}
