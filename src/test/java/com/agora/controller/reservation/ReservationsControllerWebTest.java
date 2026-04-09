package com.agora.controller.reservation;

import com.agora.config.SecurityConfig;
import com.agora.dto.request.reservation.CreateReservationRequestDto;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.reservation.ReservationDetailResponseDto;
import com.agora.dto.response.reservation.ReservationResourceDto;
import com.agora.dto.response.reservation.ReservationSummaryResponseDto;
import com.agora.enums.reservation.DepositStatus;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.enums.resource.ResourceType;
import com.agora.exception.reservation.ReservationForbiddenNoGroupException;
import com.agora.exception.reservation.SlotUnavailableException;
import com.agora.service.auth.JwtService;
import com.agora.service.reservation.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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

@WebMvcTest(ReservationsController.class)
@Import(SecurityConfig.class)
@Tag("security-web")
class ReservationsControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "user@example.com")
    void getMyReservations_shouldReturn200AndPagedContent() throws Exception {
        ReservationSummaryResponseDto item = new ReservationSummaryResponseDto(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Salle des fetes",
                ResourceType.IMMOBILIER,
                LocalDate.of(2026, 4, 10),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                ReservationStatus.CONFIRMED,
                DepositStatus.DEPOSIT_PENDING,
                7500,
                15000,
                "Reduction 50%",
                Instant.parse("2026-03-24T11:00:00Z")
        );
        PagedResponse<ReservationSummaryResponseDto> response = new PagedResponse<>(
                List.of(item),
                1L,
                1,
                0,
                20
        );
        when(reservationService.getMyReservations(any(), any(), any(Integer.class), any(Integer.class))).thenReturn(response);

        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.content[0].resourceName").value("Salle des fetes"))
                .andExpect(jsonPath("$.content[0].slotStart").value("14:00"))
                .andExpect(jsonPath("$.content[0].slotEnd").value("18:00"))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getMyReservations_shouldHandleStatusFilter() throws Exception {
        PagedResponse<ReservationSummaryResponseDto> response = new PagedResponse<>(
                List.of(),
                0L,
                0,
                0,
                20
        );
        when(reservationService.getMyReservations(any(), any(), any(Integer.class), any(Integer.class))).thenReturn(response);

        mockMvc.perform(get("/api/reservations")
                        .param("status", "CONFIRMED")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void createReservation_shouldReturn201() throws Exception {
        ReservationDetailResponseDto response = new ReservationDetailResponseDto(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Salle des fetes",
                ResourceType.IMMOBILIER,
                LocalDate.of(2026, 4, 10),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                ReservationStatus.CONFIRMED,
                DepositStatus.DEPOSIT_PENDING,
                7500,
                15000,
                "Reduction 50%",
                Instant.parse("2026-03-24T11:00:00Z"),
                new ReservationResourceDto(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "Salle des fetes",
                        ResourceType.IMMOBILIER,
                        250,
                        15000,
                        "https://img"
                ),
                "Jean Dupont",
                "Habitants commune",
                "Reunion associative mensuelle",
                List.of(),
                null,
                "26041000001",
                "user@example.com"
        );

        when(reservationService.createReservation(any(), any())).thenReturn(response);

        CreateReservationRequestDto request = new CreateReservationRequestDto(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                LocalDate.of(2026, 4, 10),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                "Reunion associative mensuelle",
                null
        );

        mockMvc.perform(post("/api/reservations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.resourceName").value("Salle des fetes"))
                .andExpect(jsonPath("$.resourceType").value("IMMOBILIER"))
                .andExpect(jsonPath("$.date").value("2026-04-10"))
                .andExpect(jsonPath("$.slotStart").value("14:00"))
                .andExpect(jsonPath("$.slotEnd").value("18:00"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.depositStatus").value("DEPOSIT_PENDING"))
                .andExpect(jsonPath("$.depositAmountCents").value(7500))
                .andExpect(jsonPath("$.depositAmountFullCents").value(15000))
                .andExpect(jsonPath("$.discountLabel").value("Reduction 50%"))
                .andExpect(jsonPath("$.createdAt").value("2026-03-24T11:00:00Z"))
                .andExpect(jsonPath("$.resource.id").value("22222222-2222-2222-2222-222222222222"))
                .andExpect(jsonPath("$.resource.name").value("Salle des fetes"))
                .andExpect(jsonPath("$.resource.resourceType").value("IMMOBILIER"))
                .andExpect(jsonPath("$.resource.capacity").value(250))
                .andExpect(jsonPath("$.resource.depositAmountCents").value(15000))
                .andExpect(jsonPath("$.resource.imageUrl").value("https://img"))
                .andExpect(jsonPath("$.userName").value("Jean Dupont"))
                .andExpect(jsonPath("$.groupName").value("Habitants commune"))
                .andExpect(jsonPath("$.purpose").value("Reunion associative mensuelle"))
                .andExpect(jsonPath("$.documents").isArray())
                .andExpect(jsonPath("$.documents").isEmpty())
                .andExpect(jsonPath("$.recurringGroupId").isEmpty());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void createReservation_shouldReturn403WhenForbiddenNoGroup() throws Exception {
        when(reservationService.createReservation(any(), any()))
                .thenThrow(new ReservationForbiddenNoGroupException(
                        "Aucun de vos groupes n'autorise la réservation de ressources MOBILIER."
                ));

        CreateReservationRequestDto request = new CreateReservationRequestDto(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                LocalDate.of(2026, 4, 10),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                "Reunion associative mensuelle",
                null
        );

        mockMvc.perform(post("/api/reservations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_FORBIDDEN_NO_GROUP"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void createReservation_shouldReturn409WhenSlotUnavailable() throws Exception {
        when(reservationService.createReservation(any(), any()))
                .thenThrow(new SlotUnavailableException(
                        "Le créneau 14h00-18h00 du 10/04/2026 est déjà réservé."
                ));

        CreateReservationRequestDto request = new CreateReservationRequestDto(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                LocalDate.of(2026, 4, 10),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                "Reunion associative mensuelle",
                null
        );

        mockMvc.perform(post("/api/reservations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getReservationById_shouldReturn200WithDetails() throws Exception {
        UUID reservationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ReservationDetailResponseDto response = new ReservationDetailResponseDto(
                reservationId,
                "Salle des fetes",
                ResourceType.IMMOBILIER,
                LocalDate.of(2026, 4, 10),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                ReservationStatus.CONFIRMED,
                DepositStatus.DEPOSIT_PENDING,
                7500,
                15000,
                "Aucune remise",
                Instant.parse("2026-03-24T11:00:00Z"),
                new ReservationResourceDto(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "Salle des fetes",
                        ResourceType.IMMOBILIER,
                        250,
                        15000,
                        "https://img"
                ),
                "Jean Dupont",
                "Habitants commune",
                "Reunion associative mensuelle",
                List.of(),
                null,
                "26041000001",
                "jean@example.com"
        );

        when(reservationService.getReservationById(any(UUID.class), any())).thenReturn(response);

        mockMvc.perform(get("/api/reservations/{reservationId}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.resourceName").value("Salle des fetes"))
                .andExpect(jsonPath("$.resourceType").value("IMMOBILIER"))
                .andExpect(jsonPath("$.date").value("2026-04-10"))
                .andExpect(jsonPath("$.slotStart").value("14:00"))
                .andExpect(jsonPath("$.slotEnd").value("18:00"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.userName").value("Jean Dupont"))
                .andExpect(jsonPath("$.bookingReference").value("26041000001"))
                .andExpect(jsonPath("$.userEmail").value("jean@example.com"))
                .andExpect(jsonPath("$.groupName").value("Habitants commune"))
                .andExpect(jsonPath("$.resource.id").value("22222222-2222-2222-2222-222222222222"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getReservationById_shouldReturn403WhenNotOwner() throws Exception {
        UUID reservationId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(reservationService.getReservationById(any(UUID.class), any()))
                .thenThrow(new com.agora.exception.BusinessException(
                        com.agora.exception.ErrorCode.RESERVATION_FORBIDDEN_NO_GROUP,
                        "Accès interdit à cette réservation"
                ));

        mockMvc.perform(get("/api/reservations/{reservationId}", reservationId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_FORBIDDEN_NO_GROUP"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getReservationById_shouldReturn404WhenNotFound() throws Exception {
        UUID reservationId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(reservationService.getReservationById(any(UUID.class), any()))
                .thenThrow(new com.agora.exception.BusinessException(
                        com.agora.exception.ErrorCode.RESOURCE_NOT_FOUND,
                        "Réservation introuvable"
                ));

        mockMvc.perform(get("/api/reservations/{reservationId}", reservationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GEN-003"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void cancelReservation_shouldReturn204WhenSuccess() throws Exception {
        UUID reservationId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        doNothing().when(reservationService).cancelReservation(any(UUID.class), any());

        mockMvc.perform(delete("/api/reservations/{reservationId}", reservationId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void cancelReservation_shouldReturn404WhenNotFound() throws Exception {
        UUID reservationId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        doThrow(new com.agora.exception.BusinessException(
                com.agora.exception.ErrorCode.RESOURCE_NOT_FOUND,
                "Réservation introuvable"
        )).when(reservationService).cancelReservation(any(UUID.class), any());

        mockMvc.perform(delete("/api/reservations/{reservationId}", reservationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GEN-003"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void cancelReservation_shouldReturn403WhenNotOwner() throws Exception {
        UUID reservationId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        doThrow(new com.agora.exception.BusinessException(
                com.agora.exception.ErrorCode.RESERVATION_FORBIDDEN_NO_GROUP,
                "Accès interdit à cette réservation"
        )).when(reservationService).cancelReservation(any(UUID.class), any());

        mockMvc.perform(delete("/api/reservations/{reservationId}", reservationId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_FORBIDDEN_NO_GROUP"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void cancelReservation_shouldReturn400WhenAlreadyCancelled() throws Exception {
        UUID reservationId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        doThrow(new com.agora.exception.BusinessException(
                com.agora.exception.ErrorCode.RESOURCE_NOT_FOUND,
                "Cette réservation est déjà annulée"
        )).when(reservationService).cancelReservation(any(UUID.class), any());

        mockMvc.perform(delete("/api/reservations/{reservationId}", reservationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GEN-003"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void cancelReservation_shouldReturn400WhenRejected() throws Exception {
        UUID reservationId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        doThrow(new com.agora.exception.BusinessException(
                com.agora.exception.ErrorCode.RESOURCE_NOT_FOUND,
                "Impossible d'annuler une réservation rejetée"
        )).when(reservationService).cancelReservation(any(UUID.class), any());

        mockMvc.perform(delete("/api/reservations/{reservationId}", reservationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GEN-003"));
    }
}
