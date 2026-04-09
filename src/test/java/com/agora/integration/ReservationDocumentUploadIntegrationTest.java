package com.agora.integration;

import com.agora.entity.reservation.Reservation;
import com.agora.testsupport.TestBookingRefs;
import com.agora.entity.resource.Resource;
import com.agora.entity.user.User;
import com.agora.enums.reservation.DepositStatus;
import com.agora.enums.reservation.ReservationStatus;
import com.agora.enums.resource.ResourceType;
import com.agora.enums.user.AccountStatus;
import com.agora.enums.user.AccountType;
import com.agora.repository.audit.AuditLogRepository;
import com.agora.repository.reservation.ReservationDocumentRepository;
import com.agora.repository.reservation.ReservationRepository;
import com.agora.repository.resource.ResourceRepository;
import com.agora.repository.user.UserRepository;
import com.agora.testutil.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "security-real-it"})
@TestPropertySource(
        properties = {
                "spring.datasource.url=jdbc:h2:mem:agora_pj_upload_it;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "spring.flyway.enabled=false",
        }
)
@Tag("integration-pj-upload")
class ReservationDocumentUploadIntegrationTest {

    private static final String OWNER_EMAIL = "pj-owner@integration.test";
    private static final String OTHER_EMAIL = "pj-other@integration.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationDocumentRepository reservationDocumentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @BeforeEach
    void cleanAndSeed() {
        auditLogRepository.deleteAll();
        reservationDocumentRepository.deleteAll();
        reservationRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        User owner = new User();
        owner.setEmail(OWNER_EMAIL);
        owner.setFirstName("PJ");
        owner.setLastName("Owner");
        owner.setPhone("0600000000");
        owner.setAccountType(AccountType.AUTONOMOUS);
        owner.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(owner);

        User other = new User();
        other.setEmail(OTHER_EMAIL);
        other.setFirstName("Other");
        other.setLastName("User");
        other.setPhone("0600000001");
        other.setAccountType(AccountType.AUTONOMOUS);
        other.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(other);

        Resource resource = Resource.builder()
                .name("Salle PJ IT")
                .resourceType(ResourceType.IMMOBILIER)
                .capacity(50)
                .depositAmountCents(0)
                .active(true)
                .build();
        resourceRepository.save(resource);

        Reservation reservation = new Reservation();
        reservation.setResource(resource);
        reservation.setUser(owner);
        reservation.setReservationDate(LocalDate.of(2026, 7, 1));
        reservation.setSlotStart(LocalTime.of(9, 0));
        reservation.setSlotEnd(LocalTime.of(10, 0));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPurpose("Integration PJ");
        reservation.setDepositStatus(DepositStatus.DEPOSIT_PENDING);
        reservation.setBookingReference(TestBookingRefs.next());
        reservationRepository.save(reservation);
    }

    @Test
    void uploadDocument_owner_shouldCreateMetadataAndReturn201() throws Exception {
        Reservation reservation = reservationRepository.findAll().getFirst();
        String token = testJwtUtil.createValidUserToken(OWNER_EMAIL);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "piece.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-integration".getBytes()
        );

        mockMvc.perform(multipart("/api/reservations/{reservationId}/documents", reservation.getId())
                        .file(file)
                        .param("docType", "OTHER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("piece.pdf"))
                .andExpect(jsonPath("$.mimeType").value(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(jsonPath("$.status").value("SENT"));

        assertThat(reservationDocumentRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log ->
                        "RESERVATION_DOCUMENT_UPLOADED".equals(log.getAction())
                                && OWNER_EMAIL.equals(log.getAdminUser())
                                && log.getDetails() != null
                                && "BREVO_SIMULATED".equals(
                                String.valueOf(log.getDetails().get("relayChannel"))));
    }

    @Test
    void uploadDocument_wrongMime_shouldReturn400() throws Exception {
        Reservation reservation = reservationRepository.findAll().getFirst();
        String token = testJwtUtil.createValidUserToken(OWNER_EMAIL);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bad.exe",
                "application/x-msdownload",
                new byte[] {0x4d, 0x5a}
        );

        mockMvc.perform(multipart("/api/reservations/{reservationId}/documents", reservation.getId())
                        .file(file)
                        .param("docType", "OTHER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GEN-001"));

        assertThat(reservationDocumentRepository.findAll()).isEmpty();
    }

    @Test
    void uploadDocument_notOwner_shouldReturn403() throws Exception {
        Reservation reservation = reservationRepository.findAll().getFirst();
        String token = testJwtUtil.createValidUserToken(OTHER_EMAIL);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "piece.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1".getBytes()
        );

        mockMvc.perform(multipart("/api/reservations/{reservationId}/documents", reservation.getId())
                        .file(file)
                        .param("docType", "OTHER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(reservationDocumentRepository.findAll()).isEmpty();
    }
}
