package com.agora.integration;

import com.agora.entity.group.Group;
import com.agora.repository.group.GroupRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garanties "front" :
 * - le preflight CORS (OPTIONS) ne doit pas être bloqué par Spring Security
 * - register/login doivent rester publics
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "security-real-it"})
@Tag("integration-cors")
class AuthCorsIntegrationTest {

    private static final String ORIGIN = "http://localhost:59470";
    private static final String PUBLIC_GROUP_NAME = "Public";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupRepository groupRepository;

    @BeforeEach
    void ensurePublicGroupExists() {
        groupRepository.findByName(PUBLIC_GROUP_NAME).orElseGet(() -> {
            Group group = new Group();
            group.setName(PUBLIC_GROUP_NAME);
            group.setPreset(true);
            return groupRepository.save(group);
        });
    }

    @Test
    void options_register_shouldBeAllowedWithCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/auth/register")
                        .header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void register_shouldRemainPublic() throws Exception {
        // Le DTO réel peut évoluer ; ce test vérifie surtout qu'on ne retombe pas sur un 403.
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "cors.register.test@agora.local",
                                  "password": "Password123!",
                                  "firstName": "Cors",
                                  "lastName": "Test",
                                  "phone": "0600000009"
                                }
                                """))
                .andExpect(status().isCreated());
    }
}

