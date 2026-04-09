package com.agora.integration;

import com.agora.entity.resource.Resource;
import com.agora.enums.resource.ResourceType;
import com.agora.repository.resource.ResourceRepository;
import com.agora.testutil.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "security-real-it"})
@Tag("integration-resources-visibility")
class ResourcesVisibilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @BeforeEach
    void setUpData() {
        resourceRepository.deleteAll();

        resourceRepository.save(Resource.builder()
                .name("Active res")
                .resourceType(ResourceType.IMMOBILIER)
                .capacity(10)
                .depositAmountCents(0)
                .active(true)
                .build());

        resourceRepository.save(Resource.builder()
                .name("Inactive res")
                .resourceType(ResourceType.MOBILIER)
                .capacity(1)
                .depositAmountCents(0)
                .active(false)
                .build());
    }

    @Test
    void getResources_anonymous_shouldSeeOnlyActive() throws Exception {
        mockMvc.perform(get("/api/resources?page=0&size=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void getResources_admin_shouldSeeActiveAndInactive() throws Exception {
        String token = testJwtUtil.createToken(
                "admin@agora.local",
                java.util.List.of("ROLE_SECRETARY_ADMIN", "ROLE_SUPERADMIN")
        );

        mockMvc.perform(get("/api/resources?page=0&size=50")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }
}

