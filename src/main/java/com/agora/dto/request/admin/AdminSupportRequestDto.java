package com.agora.dto.request.admin;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdminSupportRequestDto(
        @NotNull
        UUID userId
) {
}
