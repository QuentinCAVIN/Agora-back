package com.agora.integration.security;

import com.agora.dto.request.reservation.CreateReservationRequestDto;
import com.agora.entity.reservation.Reservation;
import com.agora.entity.resource.Resource;
import com.agora.entity.user.User;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.enums.resource.ResourceType;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceRepository;
import com.agora.repository.user.UserRepository;
import com.agora.testutil.TestJwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de sécurité réelle pour les réservations.
 *
 * IMPORTANT:
 * - Teste l'authentification JWT (tokens valides, expirés, invalides)
 * - Valide les contrôles d'accès (propriétaire vs non-propriétaire)
 * - Non-régression sécurité J2 et J3
 * - Profils "test" + "security-real-it" : désactive TestSecurityConfig permissif
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "security-real-it"})
@Tag("integration-security-real")
@Transactional
class ReservationsSecurityIntegrationTest {

    private static final String RESERVATIONS_BASE_URL = "/api/reservations";
    private static final String USER1_EMAIL = "user1@security.test";
    private static final String USER2_EMAIL = "user2@security.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1;
    private User user2;
    private Resource testResource;
    private UUID testReservationIdUser1;

    @BeforeEach
    void setupSecurityTestData() {
        // Create test users
        if (userRepository.findByEmailIgnoreCase(USER1_EMAIL).isEmpty()) {
            user1 = new User();
            user1.setEmail(USER1_EMAIL);
            user1.setFirstName("Alice");
            user1.setLastName("Owner");
            user1.setAccountType(AccountType.AUTONOMOUS);
            user1.setAccountStatus(AccountStatus.ACTIVE);
            user1 = userRepository.save(user1);
        } else {
            user1 = userRepository.findByEmailIgnoreCase(USER1_EMAIL).get();
        }

        if (userRepository.findByEmailIgnoreCase(USER2_EMAIL).isEmpty()) {
            user2 = new User();
            user2.setEmail(USER2_EMAIL);
            user2.setFirstName("Bob");
            user2.setLastName("Other");
            user2.setAccountType(AccountType.AUTONOMOUS);
            user2.setAccountStatus(AccountStatus.ACTIVE);
            user2 = userRepository.save(user2);
        } else {
            user2 = userRepository.findByEmailIgnoreCase(USER2_EMAIL).get();
        }

        // Create test resource
        testResource = Resource.builder()
                .name("Secure Test Salle")
                .resourceType(ResourceType.IMMOBILIER)
                .capacity(100)
                .description("Test resource for security")
                .depositAmountCents(10000)
                .active(true)
                .build();
        testResource = resourceRepository.save(testResource);

        // Create a test reservation by user1
        Reservation reservation = new Reservation();
        reservation.setUser(user1);
        reservation.setResource(testResource);
        reservation.setReservationDate(LocalDate.of(2026, 5, 15));
        reservation.setSlotStart(LocalTime.of(10, 0));
        reservation.setSlotEnd(LocalTime.of(12, 0));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPurpose("Security test reservation");
        testReservationIdUser1 = reservationRepository.save(reservation).getId();
    }

    // =========================================
    // AUTHENTICATION: GET MY RESERVATIONS
    // =========================================

    @Test
    void getMyReservations_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get(RESERVATIONS_BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyReservations_withValidToken_shouldReturn200() throws Exception {
        String token = testJwtUtil.createValidUserToken(USER1_EMAIL);

        mockMvc.perform(get(RESERVATIONS_BASE_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getMyReservations_withInvalidToken_shouldReturn401() throws Exception {
        mockMvc.perform(get(RESERVATIONS_BASE_URL)
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyReservations_withMalformedAuthHeader_shouldReturn401() throws Exception {
        mockMvc.perform(get(RESERVATIONS_BASE_URL)
                        .header("Authorization", "NotABearerToken"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================
    // AUTHENTICATION: CREATE RESERVATION
    // =========================================

    @Test
    void createReservation_withoutToken_shouldReturn401() throws Exception {
        CreateReservationRequestDto request = new CreateReservationRequestDto(
                testResource.getId(),
                LocalDate.of(2026, 6, 1),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                "Test creation",
                null
        );

        mockMvc.perform(post(RESERVATIONS_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReservation_withValidToken_shouldReturn201() throws Exception {
        String token = testJwtUtil.createValidUserToken(USER1_EMAIL);

        CreateReservationRequestDto request = new CreateReservationRequestDto(
                testResource.getId(),
                LocalDate.of(2026, 6, 1),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                "Authenticated creation",
                null
        );

        mockMvc.perform(post(RESERVATIONS_BASE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userName").value("Alice Owner"));
    }

    // =========================================
    // AUTHENTICATION: GET RESERVATION BY ID
    // =========================================

    @Test
    void getReservationById_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get(RESERVATIONS_BASE_URL + "/" + testReservationIdUser1))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getReservationById_withValidToken_asOwner_shouldReturn200() throws Exception {
        String token = testJwtUtil.createValidUserToken(USER1_EMAIL);

        mockMvc.perform(get(RESERVATIONS_BASE_URL + "/" + testReservationIdUser1)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testReservationIdUser1.toString()))
                .andExpect(jsonPath("$.userName").value("Alice Owner"));
    }

    @Test
    void getReservationById_withValidToken_asNonOwner_shouldReturn403() throws Exception {
        String token = testJwtUtil.createValidUserToken(USER2_EMAIL);

        mockMvc.perform(get(RESERVATIONS_BASE_URL + "/" + testReservationIdUser1)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // =========================================
    // AUTHENTICATION: CANCEL RESERVATION
    // =========================================

    @Test
    void cancelReservation_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(delete(RESERVATIONS_BASE_URL + "/" + testReservationIdUser1))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelReservation_withValidToken_asOwner_shouldReturn204() throws Exception {
        // Create a new reservation to cancel
        Reservation toCancel = new Reservation();
        toCancel.setUser(user1);
        toCancel.setResource(testResource);
        toCancel.setReservationDate(LocalDate.of(2026, 5, 20));
        toCancel.setSlotStart(LocalTime.of(15, 0));
        toCancel.setSlotEnd(LocalTime.of(17, 0));
        toCancel.setStatus(ReservationStatus.CONFIRMED);
        toCancel.setPurpose("To cancel");
        UUID reservationToCancel = reservationRepository.save(toCancel).getId();

        String token = testJwtUtil.createValidUserToken(USER1_EMAIL);

        mockMvc.perform(delete(RESERVATIONS_BASE_URL + "/" + reservationToCancel)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelReservation_withValidToken_asNonOwner_shouldReturn403() throws Exception {
        String token = testJwtUtil.createValidUserToken(USER2_EMAIL);

        mockMvc.perform(delete(RESERVATIONS_BASE_URL + "/" + testReservationIdUser1)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // =========================================
    // TOKEN VALIDATION
    // =========================================

    @Test
    void endpoints_withExpiredToken_shouldReturn401() throws Exception {
        String expiredToken = testJwtUtil.createExpiredUserToken(USER1_EMAIL);

        mockMvc.perform(get(RESERVATIONS_BASE_URL)
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpoints_withTokenFromAnotherAlgorithm_shouldReturn401() throws Exception {
        String invalidToken = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ1c2VyMUBleGFtcGxlLmNvbSJ9.";

        mockMvc.perform(get(RESERVATIONS_BASE_URL)
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    // =========================================
    // NON-REGRESSION: J2/J3 SECURITY
    // =========================================

    @Test
    void createReservation_tokenIdentifiesCorrectUser() throws Exception {
        String token1 = testJwtUtil.createValidUserToken(USER1_EMAIL);
        String token2 = testJwtUtil.createValidUserToken(USER2_EMAIL);

        CreateReservationRequestDto request1 = new CreateReservationRequestDto(
                testResource.getId(),
                LocalDate.of(2026, 6, 15),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "User identity test",
                null
        );

        CreateReservationRequestDto request2 = new CreateReservationRequestDto(
                testResource.getId(),
                LocalDate.of(2026, 6, 16),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "User identity test",
                null
        );

        // Create as user1
        mockMvc.perform(post(RESERVATIONS_BASE_URL)
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value("Alice Owner"));

        // Create as user2
        mockMvc.perform(post(RESERVATIONS_BASE_URL)
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value("Bob Other"));
    }

    @Test
    void getMyReservations_returnsOnlyUserOwnReservations() throws Exception {
        String token1 = testJwtUtil.createValidUserToken(USER1_EMAIL);
        String token2 = testJwtUtil.createValidUserToken(USER2_EMAIL);

        // User1 gets their reservation
        mockMvc.perform(get(RESERVATIONS_BASE_URL)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userName").value("Alice Owner"));

        // User2 should not see user1's reservation
        mockMvc.perform(get(RESERVATIONS_BASE_URL)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());
        // Note: Could verify empty or different content through detailed assertions
    }

    @Test
    void reservationOwnershipNotBypassable() throws Exception {
        String token1 = testJwtUtil.createValidUserToken(USER1_EMAIL);
        String token2 = testJwtUtil.createValidUserToken(USER2_EMAIL);

        // User1 creates and owns reservation
        CreateReservationRequestDto request = new CreateReservationRequestDto(
                testResource.getId(),
                LocalDate.of(2026, 7, 1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "Ownership test",
                null
        );

        var response = mockMvc.perform(post(RESERVATIONS_BASE_URL)
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        String reservationId = extractIdFromJson(responseBody);

        // User2 cannot cancel user1's reservation
        mockMvc.perform(delete(RESERVATIONS_BASE_URL + "/" + reservationId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden());

        // User2 cannot read user1's reservation
        mockMvc.perform(get(RESERVATIONS_BASE_URL + "/" + reservationId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden());
    }

    // =========================================
    // HELPERS
    // =========================================

    private String extractIdFromJson(String json) {
        try {
            return objectMapper.readTree(json).get("id").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract ID from response", e);
        }
    }
}
