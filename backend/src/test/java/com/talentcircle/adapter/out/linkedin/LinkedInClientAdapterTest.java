package com.talentcircle.adapter.out.linkedin;

import com.talentcircle.domain.model.Publication;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para LinkedInClientAdapter usando MockWebServer.
 * No requieren credenciales reales ni conexión a internet.
 *
 * Cubre los 6 casos de la Tarea 10.B:
 *   1. publishPost() exitoso → devuelve ID del header X-RestLi-Id
 *   2. publishPost() con 401 → lanza LinkedInTokenExpiredException
 *   3. publishPost() con 429 → lanza LinkedInApiException
 *   4. checkStatus() con lifecycleState=PUBLISHED → devuelve SUCCESS
 *   5. validateConnection() con token válido → devuelve true
 *   6. validateConnection() con token vacío → devuelve false sin llamar a la API
 */
class LinkedInClientAdapterTest {

    private MockWebServer mockWebServer;
    private LinkedInClientAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        // Quitar la barra final para que coincida con el formato de WebClient
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        // Usar el constructor package-private que acepta WebClient inyectado
        adapter = new LinkedInClientAdapter("test-token", "person123", webClient) {
            @Override
            void sleep(java.time.Duration duration) {
                // No esperar en tests — backoff instantáneo
            }
        };
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Caso 1: publishPost() exitoso
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("publishPost() exitoso devuelve el ID del header X-RestLi-Id")
    void publishPost_success_returnsPostId() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .addHeader("X-RestLi-Id", "urn:li:ugcPost:7123456789")
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));

        String postId = adapter.publishPost("Contenido de prueba para LinkedIn");

        assertThat(postId).isEqualTo("urn:li:ugcPost:7123456789");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/ugcPosts");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-token");
        assertThat(request.getHeader("X-Restli-Protocol-Version")).isEqualTo("2.0.0");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Caso 2: publishPost() con 401 → token expirado
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("publishPost() con 401 lanza LinkedInTokenExpiredException sin reintentar")
    void publishPost_unauthorized_throwsTokenExpiredException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"message\": \"Unauthorized\"}"));

        assertThatThrownBy(() -> adapter.publishPost("Contenido"))
                .isInstanceOf(LinkedInTokenExpiredException.class);

        // Solo debe haber 1 request — no reintenta con 401
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Caso 3: publishPost() con 429 (rate limit) → lanza LinkedInApiException
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("publishPost() con 429 lanza LinkedInApiException después de 3 reintentos")
    void publishPost_rateLimited_throwsApiExceptionAfterRetries() {
        // Encolar 3 respuestas 429 (una por cada intento)
        for (int i = 0; i < 3; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(429)
                    .addHeader("Content-Type", "application/json")
                    .setBody("{\"message\": \"Too Many Requests\"}"));
        }

        assertThatThrownBy(() -> adapter.publishPost("Contenido"))
                .isInstanceOf(LinkedInClientAdapter.LinkedInApiException.class)
                .hasMessageContaining("3 intentos");

        // Debe haber exactamente 3 requests (MAX_RETRIES)
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Caso 4: checkStatus() con lifecycleState=PUBLISHED → SUCCESS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("checkStatus() con lifecycleState=PUBLISHED devuelve SUCCESS")
    void checkStatus_published_returnsSuccess() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"lifecycleState\": \"PUBLISHED\", \"id\": \"urn:li:ugcPost:7123456789\"}"));

        Publication.PublicationStatus status = adapter.checkStatus("urn:li:ugcPost:7123456789");

        assertThat(status).isEqualTo(Publication.PublicationStatus.SUCCESS);

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-token");
    }

    @Test
    @DisplayName("checkStatus() con lifecycleState=DRAFT devuelve RETRYING")
    void checkStatus_draft_returnsRetrying() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"lifecycleState\": \"DRAFT\"}"));

        Publication.PublicationStatus status = adapter.checkStatus("urn:li:ugcPost:999");

        assertThat(status).isEqualTo(Publication.PublicationStatus.RETRYING);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Caso 5: validateConnection() con token válido → true
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateConnection() con token válido devuelve true")
    void validateConnection_validToken_returnsTrue() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"id\": \"person123\", \"localizedFirstName\": \"Javier\"}"));

        boolean valid = adapter.validateConnection("valid-token");

        assertThat(valid).isTrue();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/me");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer valid-token");
    }

    @Test
    @DisplayName("validateConnection() con respuesta 401 devuelve false")
    void validateConnection_invalidToken_returnsFalse() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"message\": \"Unauthorized\"}"));

        boolean valid = adapter.validateConnection("expired-token");

        assertThat(valid).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Caso 6: validateConnection() con token vacío → false sin llamar a la API
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateConnection() con token vacío devuelve false sin llamar a la API")
    void validateConnection_emptyToken_returnsFalseWithoutApiCall() {
        boolean validEmpty = adapter.validateConnection("");
        boolean validNull  = adapter.validateConnection(null);
        boolean validBlank = adapter.validateConnection("   ");

        assertThat(validEmpty).isFalse();
        assertThat(validNull).isFalse();
        assertThat(validBlank).isFalse();

        // No debe haber ningún request al servidor
        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Caso extra: publishPost() con credenciales no configuradas
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("publishPost() sin accessToken lanza LinkedInApiException inmediatamente")
    void publishPost_missingToken_throwsImmediately() {
        LinkedInClientAdapter adapterSinToken = new LinkedInClientAdapter("", "person123",
                WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build()) {
            @Override void sleep(java.time.Duration d) {}
        };

        assertThatThrownBy(() -> adapterSinToken.publishPost("Contenido"))
                .isInstanceOf(LinkedInClientAdapter.LinkedInApiException.class)
                .hasMessageContaining("LINKEDIN_ACCESS_TOKEN");

        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("publishPost() sin personId lanza LinkedInApiException inmediatamente")
    void publishPost_missingPersonId_throwsImmediately() {
        LinkedInClientAdapter adapterSinPersonId = new LinkedInClientAdapter("token", "",
                WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build()) {
            @Override void sleep(java.time.Duration d) {}
        };

        assertThatThrownBy(() -> adapterSinPersonId.publishPost("Contenido"))
                .isInstanceOf(LinkedInClientAdapter.LinkedInApiException.class)
                .hasMessageContaining("LINKEDIN_PERSON_ID");

        assertThat(mockWebServer.getRequestCount()).isZero();
    }
}
