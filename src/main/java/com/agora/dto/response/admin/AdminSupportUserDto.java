package com.agora.dto.response.admin;

import com.agora.enums.user.AccountStatus;

import java.util.UUID;

public record AdminSupportUserDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        AccountStatus status
) {
}
