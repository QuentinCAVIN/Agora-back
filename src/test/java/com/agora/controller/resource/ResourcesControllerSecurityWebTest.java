package com.agora.controller.resource;

import com.agora.config.SecurityConfig;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.resource.ResourceDto;
import com.agora.dto.response.resource.TimeSlotDto;
import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.service.auth.JwtService;
import com.agora.service.resource.ResourceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourcesController.class)
@Import(SecurityConfig.class)
@Tag("security-web")
class ResourcesControllerSecurityWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getResources_withoutAuthentication_returnsOk() throws Exception {
        when(resourceService.getResources(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk());
    }

    @Test
    void getSlots_withValidDate_returnsAvailableSlots() throws Exception {
        UUID resourceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LocalDate date = LocalDate.of(2026, 4, 10);

        List<TimeSlotDto> slots = List.of(
                new TimeSlotDto("08:00", "09:00", true),
                new TimeSlotDto("09:00", "10:00", false),
                new TimeSlotDto("14:00", "15:00", true)
        );

        when(resourceService.getSlots(resourceId, date)).thenReturn(slots);

        mockMvc.perform(get("/api/resources/{resourceId}/slots", resourceId)
                        .param("date", "2026-04-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotStart").value("08:00"))
                .andExpect(jsonPath("$[0].slotEnd").value("09:00"))
                .andExpect(jsonPath("$[0].isAvailable").value(true))
                .andExpect(jsonPath("$[1].slotStart").value("09:00"))
                .andExpect(jsonPath("$[1].slotEnd").value("10:00"))
                .andExpect(jsonPath("$[1].isAvailable").value(false))
                .andExpect(jsonPath("$[2].slotStart").value("14:00"))
                .andExpect(jsonPath("$[2].slotEnd").value("15:00"))
                .andExpect(jsonPath("$[2].isAvailable").value(true));
    }

    @Test
    void getSlots_withoutAuthentication_returnsOk() throws Exception {
        UUID resourceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LocalDate date = LocalDate.of(2026, 4, 10);

        when(resourceService.getSlots(resourceId, date))
                .thenReturn(List.of(
                        new TimeSlotDto("08:00", "09:00", true)
                ));

        mockMvc.perform(get("/api/resources/{resourceId}/slots", resourceId)
                        .param("date", "2026-04-10"))
                .andExpect(status().isOk());
    }

    @Test
    void getSlots_shouldReturn404WhenResourceNotFound() throws Exception {
        UUID resourceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LocalDate date = LocalDate.of(2026, 4, 10);

        when(resourceService.getSlots(resourceId, date))
                .thenThrow(new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Ressource introuvable"
                ));

        mockMvc.perform(get("/api/resources/{resourceId}/slots", resourceId)
                        .param("date", "2026-04-10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GEN-003"));
    }

    @Test
    void getSlots_shouldReturnCompleteSlotRange() throws Exception {
        UUID resourceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LocalDate date = LocalDate.of(2026, 4, 10);

        List<TimeSlotDto> slots = List.of(
                new TimeSlotDto("08:00", "09:00", true),
                new TimeSlotDto("09:00", "10:00", true),
                new TimeSlotDto("10:00", "11:00", true),
                new TimeSlotDto("11:00", "12:00", true),
                new TimeSlotDto("12:00", "13:00", true),
                new TimeSlotDto("13:00", "14:00", true),
                new TimeSlotDto("14:00", "15:00", true),
                new TimeSlotDto("15:00", "16:00", true),
                new TimeSlotDto("16:00", "17:00", true),
                new TimeSlotDto("17:00", "18:00", true)
        );

        when(resourceService.getSlots(resourceId, date)).thenReturn(slots);

        mockMvc.perform(get("/api/resources/{resourceId}/slots", resourceId)
                        .param("date", "2026-04-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].slotStart").value("08:00"))
                .andExpect(jsonPath("$[9].slotEnd").value("18:00"));
    }
}
