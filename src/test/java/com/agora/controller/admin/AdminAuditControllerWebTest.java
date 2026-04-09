package com.agora.controller.admin;

import com.agora.config.SecurityConfig;
import com.agora.dto.response.admin.AdminAuditPageResponse;
import com.agora.service.admin.AdminAuditQueryService;
import com.agora.service.auth.JwtService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuditController.class)
@Import(SecurityConfig.class)
@Tag("security-web")
class AdminAuditControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminAuditQueryService adminAuditQueryService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(roles = "SECRETARY_ADMIN")
    void list_shouldAcceptCahierQueryParams() throws Exception {
        when(adminAuditQueryService.list(
                eq(0),
                eq(20),
                eq("admin@example.com"),
                eq("target@example.com"),
                eq(true),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 30)),
                eq(null)
        )).thenReturn(new AdminAuditPageResponse(List.of(), 0, 0));

        mockMvc.perform(get("/api/admin/audit")
                        .param("page", "0")
                        .param("size", "20")
                        .param("adminUserId", "admin@example.com")
                        .param("targetUserId", "target@example.com")
                        .param("impersonationOnly", "true")
                        .param("dateFrom", "2026-04-01")
                        .param("dateTo", "2026-04-30"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SECRETARY_ADMIN")
    void list_withoutFilters_shouldDelegateDefaults() throws Exception {
        when(adminAuditQueryService.list(0, 20, null, null, null, null, null, null))
                .thenReturn(new AdminAuditPageResponse(List.of(), 0, 0));

        mockMvc.perform(get("/api/admin/audit"))
                .andExpect(status().isOk());
    }
}
