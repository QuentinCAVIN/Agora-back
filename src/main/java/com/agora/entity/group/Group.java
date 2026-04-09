package com.agora.entity.group;

import com.agora.enums.group.DiscountAppliesTo;
import com.agora.enums.group.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
public class Group {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "is_preset", nullable = false)
    private boolean preset;

    @Column(name = "can_book_immobilier", nullable = false)
    private boolean canBookImmobilier = false;

    @Column(name = "can_book_mobilier", nullable = false)
    private boolean canBookMobilier = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 40)
    private DiscountType discountType = DiscountType.NONE;

    @Column(name = "discount_value", nullable = false)
    private int discountValue = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_applies_to", nullable = false, length = 40)
    private DiscountAppliesTo discountAppliesTo = DiscountAppliesTo.ALL;

    /**
     * Groupe doté de pouvoirs « conseil municipal » (ex. validation des confirmations
     * effectuée par un personnel délégué uniquement si le compte est aussi membre de ce groupe).
     */
    @Column(name = "council_powers", nullable = false)
    private boolean councilPowers = false;
}
