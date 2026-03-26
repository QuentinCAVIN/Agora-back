package com.agora.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @LocalServerPort
    protected int port;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    // =========================
    // TOKENS (temporaire)
    // =========================

    protected String adminBearer = "Bearer test-admin-token";
    protected String userBearer = "Bearer test-user-token";

    @BeforeEach
    void setup() {

        // 👉 Base URL automatique
        String baseUrl = String.format("http://localhost:%d%s", port,
                (contextPath == null ? "" : contextPath));

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);

        restTemplate.getRestTemplate().setUriTemplateHandler(factory);

        // 👉 IMPORTANT : ne pas throw exception sur 4xx/5xx
        restTemplate.getRestTemplate().setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }
        });
    }

    // =========================
    // HEADERS
    // =========================

    protected HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    protected HttpHeaders jsonHeadersWithAdmin() {
        HttpHeaders headers = jsonHeaders();
        headers.set("Authorization", adminBearer);
        return headers;
    }

    protected HttpHeaders jsonHeadersWithUser() {
        HttpHeaders headers = jsonHeaders();
        headers.set("Authorization", userBearer);
        return headers;
    }

    // =========================
    // HELPERS HTTP
    // =========================

    protected <T> ResponseEntity<T> get(String url, Class<T> type) {
        return restTemplate.exchange(url, HttpMethod.GET, null, type);
    }

    protected <T> ResponseEntity<T> get(String url, HttpHeaders headers, Class<T> type) {
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), type);
    }

    protected <T> ResponseEntity<T> post(String url, Object body, HttpHeaders headers, Class<T> type) {
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), type);
    }

    protected <T> ResponseEntity<T> put(String url, Object body, HttpHeaders headers, Class<T> type) {
        return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), type);
    }

    protected ResponseEntity<Void> delete(String url, HttpHeaders headers) {
        return restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }
}