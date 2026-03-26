package com.agora.integration;

import com.agora.dto.request.resource.ResourceRequest;
import com.agora.dto.response.common.PagedResponse;
import com.agora.dto.response.resource.ResourceDto;
import com.agora.enums.resource.ResourceType;
import com.agora.exception.ApiError;
import com.agora.exception.ErrorCode;
import com.agora.testutil.IntegrationTestBase;
import com.agora.testutil.ResourceTestData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResourcesIntegrationTest extends IntegrationTestBase {

    // =========================================
    // CREATE
    // =========================================
    @Test
    void createResource_shouldReturn201() {

        ResourceRequest request = new ResourceRequest(
                "Salle Test",
                ResourceType.IMMOBILIER,
                100,
                "desc",
                15000,
                ResourceTestData.meetingRoomImage(),
                List.of("PMR_ACCESS")
        );

        ResponseEntity<ResourceDto> response = post(
                "/api/resources",
                request,
                jsonHeadersWithAdmin(),
                ResourceDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED); // ✅ FIX
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Salle Test");
    }

    // =========================================
    // GET ALL
    // =========================================
    @Test
    void getResources_shouldReturnPaged() {

        ResponseEntity<PagedResponse> response = get(
                "/api/resources?page=0&size=10",
                PagedResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).isNotNull();
    }

    // =========================================
    // GET BY ID NOT FOUND
    // =========================================
    @Test
    void getResource_shouldReturn404() {

        ResponseEntity<String> response = get(
                "/api/resources/" + UUID.randomUUID(),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // =========================================
    // DELETE
    // =========================================
    @Test
    void deleteResource_shouldReturn204() {

        // create first
        ResourceRequest request = new ResourceRequest(
                "Salle Delete",
                ResourceType.IMMOBILIER,
                100,
                "desc",
                15000,
                ResourceTestData.meetingRoomImage(),
                List.of()
        );

        ResponseEntity<ResourceDto> created = post(
                "/api/resources",
                request,
                jsonHeadersWithAdmin(),
                ResourceDto.class
        );

        Assertions.assertNotNull(created.getBody());
        UUID id = created.getBody().id();

        ResponseEntity<Void> response = delete(
                "/api/resources/" + id,
                jsonHeadersWithAdmin()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_fail_when_capacity_missing_for_immobilier() {

        ResourceRequest request = new ResourceRequest(
                "Salle X",
                ResourceType.IMMOBILIER,
                null,
                "desc",
                1000,
                null,
                List.of()
        );

        ResponseEntity<ApiError> response = post(
                "/api/resources",
                request,
                jsonHeadersWithAdmin(),
                ApiError.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo(ErrorCode.RESOURCE_CAPACITY_VIOLATION.code());
    }
    @Test
    void should_fail_when_invalid_tag() {

        ResourceRequest request = new ResourceRequest(
                "Salle X",
                ResourceType.IMMOBILIER,
                100,
                "desc",
                1000,
                null,
                List.of("INVALID_TAG")
        );

        ResponseEntity<ApiError> response = post(
                "/api/resources",
                request,
                jsonHeadersWithAdmin(),
                ApiError.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getCode())
                .isEqualTo(ErrorCode.RESOURCE_TAG_INVALID.code());
    }
    @Test
    void should_return_404_when_resource_not_found() {

        ResponseEntity<ApiError> response = get(
                "/api/resources/" + java.util.UUID.randomUUID(),
                ApiError.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.code());
    }
}