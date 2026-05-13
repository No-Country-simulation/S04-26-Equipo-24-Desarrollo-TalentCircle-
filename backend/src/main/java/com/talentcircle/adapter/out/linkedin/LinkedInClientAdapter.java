package com.talentcircle.adapter.out.linkedin;

import com.talentcircle.domain.model.Publication;
import com.talentcircle.domain.port.out.LinkedInClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

/**
 * Adaptador de salida que implementa LinkedInClientPort usando la API v2 de LinkedIn.
 *
 * Endpoints usados:
 *   POST https://api.linkedin.com/v2/ugcPosts      → publicar un post
 *   GET  https://api.linkedin.com/v2/ugcPosts/{id} → consultar estado
 *   GET  https://api.linkedin.com/v2/me            → validar token
 *
 * Variables de entorno requeridas:
 *   LINKEDIN_ACCESS_TOKEN  → OAuth 2.0 token con scope w_member_social
 *   LINKEDIN_PERSON_ID     → URN del perfil, ej: "ABC123"
 *
 * Reintentos: backoff exponencial 1s → 2s → 4s (máximo 3 intentos).
 */
@Component
public class LinkedInClientAdapter implements LinkedInClientPort {

    private static final Logger log = LoggerFactory.getLogger(LinkedInClientAdapter.class);

    static final String LINKEDIN_API_BASE = "https://api.linkedin.com/v2";
    private static final int MAX_RETRIES = 3;

    private final String accessToken;
    private final String personId;
    private final WebClient webClient;

    public LinkedInClientAdapter(
            @Value("${app.linkedin.access-token:}") String accessToken,
            @Value("${app.linkedin.person-id:}") String personId) {
        this(accessToken, personId,
                WebClient.builder()
                        .baseUrl(LINKEDIN_API_BASE)
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader("X-Restli-Protocol-Version", "2.0.0")
                        .build());
    }

    /** Constructor para tests — permite inyectar un WebClient con MockWebServer */
    LinkedInClientAdapter(String accessToken, String personId, WebClient webClient) {
        this.accessToken = accessToken;
        this.personId = personId;
        this.webClient = webClient;
    }

    /**
     * Publica un post de texto en LinkedIn usando la API ugcPosts.
     * Implementa backoff exponencial: 1s → 2s → 4s (máximo 3 reintentos).
     *
     * @param content texto del post (máx. 3000 caracteres)
     * @return ID externo del post creado (ej: "urn:li:ugcPost:123456789")
     * @throws LinkedInTokenExpiredException si el token es inválido o expirado (HTTP 401)
     * @throws LinkedInApiException          si falla después de todos los reintentos
     */
    @Override
    public String publishPost(String content) {
        validateCredentials();

        Map<String, Object> body = Map.of(
                "author", "urn:li:person:" + personId,
                "lifecycleState", "PUBLISHED",
                "specificContent", Map.of(
                        "com.linkedin.ugc.ShareContent", Map.of(
                                "shareCommentary", Map.of("text", content),
                                "shareMediaCategory", "NONE"
                        )
                ),
                "visibility", Map.of(
                        "com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC"
                )
        );

        log.info("Publicando post en LinkedIn para persona: {}", personId);

        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                var response = webClient.post()
                        .uri("/ugcPosts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .bodyValue(body)
                        .retrieve()
                        .onStatus(status -> status == HttpStatus.UNAUTHORIZED, r ->
                                r.bodyToMono(String.class).map(e ->
                                        new LinkedInTokenExpiredException(e)))
                        .onStatus(HttpStatusCode::is4xxClientError, r ->
                                r.bodyToMono(String.class).map(e -> {
                                    log.error("Error 4xx de LinkedIn (intento {}): {}", attempt, e);
                                    return new LinkedInApiException("LinkedIn API error (4xx): " + e);
                                }))
                        .onStatus(HttpStatusCode::is5xxServerError, r ->
                                r.bodyToMono(String.class).map(e -> {
                                    log.warn("Error 5xx de LinkedIn (intento {}): {}", attempt, e);
                                    return new LinkedInApiException("LinkedIn API error (5xx): " + e);
                                }))
                        .toBodilessEntity()
                        .block();

                if (response == null) {
                    throw new LinkedInApiException("Respuesta nula de LinkedIn API");
                }

                String postId = response.getHeaders().getFirst("X-RestLi-Id");
                if (postId == null || postId.isBlank()) {
                    String location = response.getHeaders().getFirst("Location");
                    postId = location != null ? location : "unknown-" + System.currentTimeMillis();
                }

                log.info("Post publicado exitosamente en LinkedIn. ID: {}", postId);
                return postId;

            } catch (LinkedInTokenExpiredException e) {
                // 401 no tiene sentido reintentar — el token está expirado
                throw e;
            } catch (LinkedInApiException e) {
                // 4xx tampoco tiene sentido reintentar (error del cliente)
                if (e.getMessage() != null && e.getMessage().contains("4xx")) {
                    throw e;
                }
                lastException = e;
                log.warn("Intento {}/{} fallido: {}. Reintentando en {}s…",
                        attempt, MAX_RETRIES, e.getMessage(), (long) Math.pow(2, attempt - 1));
            } catch (Exception e) {
                lastException = e;
                log.warn("Intento {}/{} fallido con error inesperado: {}. Reintentando en {}s…",
                        attempt, MAX_RETRIES, e.getMessage(), (long) Math.pow(2, attempt - 1));
            }

            if (attempt < MAX_RETRIES) {
                sleep(Duration.ofSeconds((long) Math.pow(2, attempt - 1))); // 1s, 2s, 4s
            }
        }

        throw new LinkedInApiException(
                "Publicación en LinkedIn falló después de " + MAX_RETRIES + " intentos: " +
                (lastException != null ? lastException.getMessage() : "error desconocido"),
                lastException);
    }

    /**
     * Consulta el estado de un post publicado.
     *
     * @param externalPostId ID del post (ej: "urn:li:ugcPost:123456789")
     * @return estado de la publicación
     */
    @Override
    public Publication.PublicationStatus checkStatus(String externalPostId) {
        validateCredentials();

        log.debug("Consultando estado del post: {}", externalPostId);

        try {
            String encodedId = java.net.URLEncoder.encode(externalPostId,
                    java.nio.charset.StandardCharsets.UTF_8);

            var response = webClient.get()
                    .uri("/ugcPosts/{id}", encodedId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.UNAUTHORIZED, r ->
                            r.bodyToMono(String.class).map(LinkedInTokenExpiredException::new))
                    .onStatus(HttpStatusCode::is4xxClientError, r ->
                            r.bodyToMono(String.class).map(e ->
                                    new LinkedInApiException("Error consultando post: " + e)))
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return Publication.PublicationStatus.FAILED;
            }

            String lifecycleState = (String) response.get("lifecycleState");
            return "PUBLISHED".equals(lifecycleState)
                    ? Publication.PublicationStatus.SUCCESS
                    : Publication.PublicationStatus.RETRYING;

        } catch (LinkedInTokenExpiredException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error consultando estado del post {} en LinkedIn", externalPostId, e);
            return Publication.PublicationStatus.FAILED;
        }
    }

    /**
     * Valida que el access token sea válido llamando a GET /me.
     * Devuelve el personId si el token es válido, null si no.
     *
     * @param token token a validar (puede ser distinto al configurado)
     * @return true si el token es válido
     */
    @Override
    public boolean validateConnection(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            var response = webClient.get()
                    .uri("/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class).map(e ->
                                    new LinkedInApiException("Token inválido: " + e)))
                    .bodyToMono(Map.class)
                    .block();

            boolean valid = response != null && response.containsKey("id");
            log.info("Validación de token LinkedIn: {}", valid ? "OK" : "FALLIDA");
            return valid;

        } catch (Exception e) {
            log.warn("Token LinkedIn inválido o expirado: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Valida el token y devuelve el personId si es válido.
     * Usado por el endpoint POST /api/v1/admin/linkedin/validate.
     *
     * @param token token a validar
     * @return personId del perfil, o null si el token es inválido
     */
    public String validateAndGetPersonId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            var response = webClient.get()
                    .uri("/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class).map(e ->
                                    new LinkedInApiException("Token inválido: " + e)))
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                return (String) response.get("id");
            }
            return null;

        } catch (Exception e) {
            log.warn("Token LinkedIn inválido: {}", e.getMessage());
            return null;
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void validateCredentials() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new LinkedInApiException(
                    "LINKEDIN_ACCESS_TOKEN no configurado. " +
                    "Agrega la variable de entorno o app.linkedin.access-token en application.yml");
        }
        if (personId == null || personId.isBlank()) {
            throw new LinkedInApiException(
                    "LINKEDIN_PERSON_ID no configurado. " +
                    "Agrega la variable de entorno o app.linkedin.person-id en application.yml");
        }
    }

    /** Pausa el hilo — extraído para facilitar tests */
    void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Excepción específica para errores de la API de LinkedIn */
    public static class LinkedInApiException extends RuntimeException {
        public LinkedInApiException(String message) { super(message); }
        public LinkedInApiException(String message, Throwable cause) { super(message, cause); }
    }
}
