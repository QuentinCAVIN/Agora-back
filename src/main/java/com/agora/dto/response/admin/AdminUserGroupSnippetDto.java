package com.agora.dto.response.admin;

public record AdminUserGroupSnippetDto(
        String id,
        String name,
        String discountLabel,
        boolean councilPowers,
        boolean canBookImmobilier,
        boolean canBookMobilier
) {
}
