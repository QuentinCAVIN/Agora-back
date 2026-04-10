package com.agora.dto.response.admin;

import java.util.List;

public record AdminUserDetailResponseDto(
        String id,
        String email,
        String firstName,
        String lastName,
        String accountType,
        String status,
        String phone,
        String internalRef,
        String notesAdmin,
        List<AdminUserGroupSnippetDto> groups,
        List<String> exemptions,
        String createdAt
) {
}
