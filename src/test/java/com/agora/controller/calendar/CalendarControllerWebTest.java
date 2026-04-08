package com.agora.controller.calendar;

import com.agora.config.SecurityConfig;
import com.agora.dto.response.calendar.CalendarDayDto;
import com.agora.dto.response.calendar.CalendarResponseDto;
import com.agora.dto.response.calendar.CalendarSlotDto;
import com.agora.enums.resource.ResourceType;
import com.agora.service.auth.JwtService;
import com.agora.service.calendar.CalendarService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalendarController.class)
@Import(SecurityConfig.class)
@Tag("security-web")
class CalendarControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalendarService calendarService;

    @MockBean
    private JwtService jwtService;

    @Test
    void getCalendar_withoutAuth_returnsOk() throws Exception {
        UUID rid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        CalendarResponseDto body = new CalendarResponseDto(
                2026,
                4,
                List.of(new CalendarDayDto(
                        LocalDate.of(2026, 4, 10),
                        false,
                        null,
                        List.of(new CalendarSlotDto(
                                rid,
                                "Salle test",
                                ResourceType.IMMOBILIER,
                                "08:00",
                                "09:00",
                                true
                        ))
                ))
        );

        when(calendarService.getMonthlyCalendar(eq(2026), eq(4), isNull())).thenReturn(body);

        mockMvc.perform(get("/api/calendar").param("year", "2026").param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(4))
                .andExpect(jsonPath("$.days[0].date").value("2026-04-10"))
                .andExpect(jsonPath("$.days[0].isBlackout").value(false))
                .andExpect(jsonPath("$.days[0].slots[0].resourceName").value("Salle test"))
                .andExpect(jsonPath("$.days[0].slots[0].isAvailable").value(true));
    }
}
