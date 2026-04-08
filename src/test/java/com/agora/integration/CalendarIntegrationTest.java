package com.agora.integration;

import com.agora.testutil.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Intégration HTTP du calendrier (profil test, sécurité assouplie — voir {@link IntegrationTestBase}).
 */
class CalendarIntegrationTest extends IntegrationTestBase {

    @Test
    void getCalendar_returnsStructuredMonthlyView() {
        ResponseEntity<JsonNode> response = get("/api/calendar?year=2026&month=4", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.path("year").asInt()).isEqualTo(2026);
        assertThat(body.path("month").asInt()).isEqualTo(4);
        assertThat(body.path("days").isArray()).isTrue();
        assertThat(body.path("days")).hasSize(30);

        JsonNode firstDay = body.path("days").get(0);
        assertThat(firstDay.path("date").asText()).isEqualTo("2026-04-01");
        assertThat(firstDay.path("isBlackout").asBoolean()).isFalse();
        assertThat(firstDay.path("slots").isArray()).isTrue();
    }
}
