package com.agora.entity.resource;

import com.agora.config.AccessibilityTagConverter;
import com.agora.entity.common.Auditable;
import com.agora.enums.resource.AccessibilityTag;
import com.agora.enums.resource.ResourceType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "resources")
public class Resource extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType;

    @Column
    private Integer capacity;

    @Column(nullable = false)
    private boolean active = true;

    @Convert(converter = AccessibilityTagConverter.class)
    @Column(name = "accessibility_tags", columnDefinition = "TEXT")
    private List<AccessibilityTag> accessibilityTags;

    @Column(name = "deposit_amount_cents", nullable = false)
    private double depositAmountCents;

    @Column(name = "image_url")
    private String imageUrl;
}