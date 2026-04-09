package com.agora.service.admin;

import com.agora.dto.response.admin.AdminSupportUserDto;

import java.util.List;
import java.util.UUID;

public interface SuperadminService {

    List<AdminSupportUserDto> getActiveAdminSupportUsers();

    AdminSupportUserDto grantAdminSupport(UUID userId);

    void revokeAdminSupport(UUID userId);
}
