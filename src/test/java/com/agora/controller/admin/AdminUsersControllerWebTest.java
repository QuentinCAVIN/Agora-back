package com.agora.controller.admin;

import com.agora.config.SecurityConfig;
import com.agora.service.admin.AdminUserService;
import com.agora.service.auth.JwtService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUsersController.class)
@Import({SecurityConfig.class, AdminUsersControllerWebTest.TestMethodSecurityConfig.class})
@Tag("security-web")
class AdminUsersControllerWebTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestMethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "admin@agora.local", roles = "SECRETARY_ADMIN")
    void printSummary_withSecretaryRole_returnsPdf() throws Exception {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        byte[] pdf = new byte[] {0x25, 0x50, 0x44, 0x46}; // %PDF header minimal stub

        when(adminUserService.getUserPrintSummaryPdf(userId)).thenReturn(pdf);

        mockMvc.perform(get("/api/admin/users/{userId}/print-summary", userId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"fiche-utilisateur-" + userId + ".pdf\""))
                .andExpect(content().bytes(pdf));
    }

    @Test
    @WithMockUser(username = "citoyen@agora.local", roles = "CITIZEN")
    void printSummary_withoutStaffRole_returnsForbidden() throws Exception {
        UUID userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        mockMvc.perform(get("/api/admin/users/{userId}/print-summary", userId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "support@agora.local", roles = "ADMIN_SUPPORT")
    void printSummary_withAdminSupportRole_returnsOk() throws Exception {
        UUID userId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(adminUserService.getUserPrintSummaryPdf(any())).thenReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get("/api/admin/users/{userId}/print-summary", userId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "sec@agora.local", roles = "SECRETARY_ADMIN")
    void purge_withSecretaryRole_returnsNoContent() throws Exception {
        UUID userId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        doNothing().when(adminUserService).purgeUser(eq(userId), any(Authentication.class));

        mockMvc.perform(delete("/api/admin/users/{userId}", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "citoyen@agora.local", roles = "CITIZEN")
    void purge_asCitizen_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/admin/users/{userId}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
