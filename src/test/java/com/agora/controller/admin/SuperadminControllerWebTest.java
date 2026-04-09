package com.agora.controller.admin;

import com.agora.config.SecurityConfig;
import com.agora.dto.request.admin.AdminSupportRequestDto;
import com.agora.dto.response.admin.AdminSupportUserDto;
import com.agora.enums.user.AccountStatus;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.service.admin.SuperadminService;
import com.agora.service.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuperadminController.class)
@Import({SecurityConfig.class, SuperadminControllerWebTest.TestMethodSecurityConfig.class})
@Tag("security-web")
class SuperadminControllerWebTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestMethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SuperadminService superadminService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "superadmin@agora.local", roles = "SUPERADMIN")
    void getAdminSupportUsers_withSuperadminRole_returnsOk() throws Exception {
        when(superadminService.getActiveAdminSupportUsers()).thenReturn(List.of(
                new AdminSupportUserDto(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "paul.assiste@mairie.fr",
                        "Paul",
                        "Assiste",
                        AccountStatus.ACTIVE
                )
        ));

        mockMvc.perform(get("/api/superadmin/admin-support"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("paul.assiste@mairie.fr"))
                .andExpect(jsonPath("$[0].firstName").value("Paul"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "admin@agora.local", roles = "SECRETARY_ADMIN")
    void getAdminSupportUsers_withoutSuperadminRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/superadmin/admin-support"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "superadmin@agora.local", roles = "SUPERADMIN")
    void grantAdminSupport_withSuperadminRole_returnsCreated() throws Exception {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        AdminSupportRequestDto request = new AdminSupportRequestDto(userId);

        when(superadminService.grantAdminSupport(userId)).thenReturn(
                new AdminSupportUserDto(
                        userId,
                        "jean.dupont@gmail.com",
                        "Jean",
                        "Dupont",
                        AccountStatus.ACTIVE
                )
        );

        mockMvc.perform(post("/api/superadmin/admin-support")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("22222222-2222-2222-2222-222222222222"))
                .andExpect(jsonPath("$.email").value("jean.dupont@gmail.com"))
                .andExpect(jsonPath("$.lastName").value("Dupont"));
    }

    @Test
    @WithMockUser(username = "admin@agora.local", roles = "SECRETARY_ADMIN")
    void grantAdminSupport_withoutSuperadminRole_returnsForbidden() throws Exception {
        AdminSupportRequestDto request = new AdminSupportRequestDto(
                UUID.fromString("22222222-2222-2222-2222-222222222222")
        );

        mockMvc.perform(post("/api/superadmin/admin-support")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "superadmin@agora.local", roles = "SUPERADMIN")
    void grantAdminSupport_whenAlreadyAdminSupport_returnsConflict() throws Exception {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        AdminSupportRequestDto request = new AdminSupportRequestDto(userId);

        when(superadminService.grantAdminSupport(userId)).thenThrow(
                new BusinessException(ErrorCode.ADMIN_SUPPORT_ALREADY_EXISTS, "Cet utilisateur est déjà ADMIN_SUPPORT")
        );

        mockMvc.perform(post("/api/superadmin/admin-support")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADMIN_SUPPORT_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(username = "superadmin@agora.local", roles = "SUPERADMIN")
    void revokeAdminSupport_withSuperadminRole_returnsNoContent() throws Exception {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        doNothing().when(superadminService).revokeAdminSupport(userId);

        mockMvc.perform(delete("/api/superadmin/admin-support/{userId}", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin@agora.local", roles = "SECRETARY_ADMIN")
    void revokeAdminSupport_withoutSuperadminRole_returnsForbidden() throws Exception {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        mockMvc.perform(delete("/api/superadmin/admin-support/{userId}", userId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "superadmin@agora.local", roles = "SUPERADMIN")
    void revokeAdminSupport_whenUserNotFound_returnsNotFound() throws Exception {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        doThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Utilisateur introuvable"))
                .when(superadminService).revokeAdminSupport(any(UUID.class));

        mockMvc.perform(delete("/api/superadmin/admin-support/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GEN-003"));
    }

    @Test
    @WithMockUser(username = "superadmin@agora.local", roles = "SUPERADMIN")
    void revokeSecretaryAdmin_withSuperadminRole_returnsNoContent() throws Exception {
        UUID userId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        doNothing().when(superadminService).revokeSecretaryAdmin(userId);

        mockMvc.perform(delete("/api/superadmin/secretary-admin/{userId}", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin@agora.local", roles = "SECRETARY_ADMIN")
    void revokeSecretaryAdmin_withoutSuperadminRole_returnsForbidden() throws Exception {
        UUID userId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        mockMvc.perform(delete("/api/superadmin/secretary-admin/{userId}", userId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "superadmin@agora.local", roles = "SUPERADMIN")
    void revokeSecretaryAdmin_whenLast_returnsConflict() throws Exception {
        UUID userId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        doThrow(new BusinessException(ErrorCode.LAST_ADMIN_CONSTRAINT, "Dernier secrétaire"))
                .when(superadminService).revokeSecretaryAdmin(userId);

        mockMvc.perform(delete("/api/superadmin/secretary-admin/{userId}", userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH-011"));
    }
}
