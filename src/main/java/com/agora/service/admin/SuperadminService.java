package com.agora.service.admin;

import com.agora.dto.response.admin.AdminSupportUserDto;

import java.util.List;
import java.util.UUID;

public interface SuperadminService {

    List<AdminSupportUserDto> getActiveAdminSupportUsers();

    AdminSupportUserDto grantAdminSupport(UUID userId);

    void revokeAdminSupport(UUID userId);

    /**
     * Retire le rôle {@link com.agora.entity.user.ERole#SECRETARY_ADMIN} persisté.
     * Interdit s'il n'existe qu'un seul compte actif avec ce rôle (barème énoncé).
     */
    void revokeSecretaryAdmin(UUID userId);
}
