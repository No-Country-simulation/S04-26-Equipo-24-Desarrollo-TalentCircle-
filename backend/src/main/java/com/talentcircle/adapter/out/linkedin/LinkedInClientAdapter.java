package com.talentcircle.adapter.out.linkedin;

import com.talentcircle.domain.model.Publication;
import com.talentcircle.domain.port.out.LinkedInClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Adaptador de salida que implementa LinkedInClientPort usando la API v2 de LinkedIn.
 *
 * Endpoints usados:
 *   POST https://api.linkedin.com/v2/ugcPosts   → publicar un post
 *   GET  https://api.linkedin.com/v2/ugcPosts/{id} → consultar estado
 *   GET  https://api.linkedin.com/v2/me          → validar token
 *
 * Variables de entorno requeridas:
 *   LINKEDIN_ACCESS_TOKEN  → OAuth 2.0 token con scope w_member_social
 *   LINKEDIN_PERSON_ID     → URN del perfil, ej: "ABC123"
 */
@Component
public class LinkedInClientAdapter implements LinkedInClientPort {

    private static final Logger log = LoggerFactory.getLogger(LinkedInClientAdapter.class);

    private static final String LINKEDIN_API_BASE = "https://api.linkedin.com/v2";

    private final String accessToken;
    private final String personId;
    private final WebClient webClient;

    public LinkedInClientAdapter(
            @Value("${app.linkedin.access-token:}") String accessToken,
            @Value("${app.linkedin.person-id:}") String personId) {
        this.accessToken = accessToken;
        this.personId = personId;
        this.webClient = WebClient.builder()
                .baseUrl(LINKEDIN_API_BASE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Restli-Protocol-Version", "2.0.0")
                .build();
    }

    /**
     * Publica un post de texto en LinkedIn usando la API ugcPosts.
     *
     * @param content texto del post (máx. 3000 caracteres para posts personales)
     * @return ID externo del post creado (ej: "urn:li:ugcPost:123456789")
     */
    @Override
    public String publishPost(String content) {
        validateCredentials();

        // Cuerpo del request según la especificación UGC Posts de LinkedIn
        Map<String, Object> body = Map.of(
                "author", "urn:li:person:" + personId,
                "lifecycleState", "PUBLISHED",
                "specificContent", Map.of(
                        "com.linkedin.ugc.ShareContent", Map.of(
                                "shareCommentary", Map.of(
                                        "text", content
                                ),
                                "shareMediaCategory", "NONE"
                        )
                ),
                "visibility", Map.of(
                        "com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC"
                )
        );

        log.info("Publicando post en LinkedIn para persona: {}", personId);

        try {
            // LinkedIn devuelve el ID del post en el header "X-RestLi-Id"
            var response = webClient.post()
                    .uri("/ugcPosts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class).map(errorBody -> {
                                log.error("Error 4xx de LinkedIn: {}", errorBody);
                                return new LinkedInApiException("LinkedIn API error (4xx): " + errorBody);
                            })
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class).map(errorBody -> {
                                log.error("Error 5xx de LinkedIn: {}", errorBody);
                                return new LinkedInApiException("LinkedIn API error (5xx): " + errorBody);
                            })
                    )
                    .toBodilessEntity()
                    .block();

            if (response == null) {
                throw new LinkedInApiException("Respuesta nula de LinkedIn API");
            }

            // El ID del post viene en el header X-RestLi-Id
            String postId = response.getHeaders().getFirst("X-RestLi-Id");
            if (postId == null || postId.isBlank()) {
                // Fallback: intentar obtenerlo del header Location
                String location = response.getHeaders().getFirst("Location");
                postId = location != null ? location : "unknown-" + System.currentTimeMillis();
            }

            log.info("Post publicado exitosamente en LinkedIn. ID: {}", postId);
            return postId;

        } catch (LinkedInApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al publicar en LinkedIn", e);
            throw new LinkedInApiException("Error al publicar en LinkedIn: " + e.getMessage(), e);
        }
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
            // Encode el URN para usarlo como path param
            String encodedId = java.net.URLEncoder.encode(externalPostId, java.nio.charset.StandardCharsets.UTF_8);

            var response = webClient.get()
                    .uri("/ugcPosts/{id}", encodedId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class).map(errorBody ->
                                    new LinkedInApiException("Error consultando post: " + errorBody)
                            )
                    )
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return Publication.PublicationStatus.FAILED;
            }

            // lifecycleState: PUBLISHED = éxito, DRAFT = pendiente
            String lifecycleState = (String) response.get("lifecycleState");
            return "PUBLISHED".equals(lifecycleState)
                    ? Publication.PublicationStatus.SUCCESS
                    : Publication.PublicationStatus.RETRYING;

        } catch (Exception e) {
            log.error("Error consultando estado del post {} en LinkedIn", externalPostId, e);
            return Publication.PublicationStatus.FAILED;
        }
    }

    /**
     * Valida que el access token sea válido llamando a /me.
     *
     * @param token token a validar
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
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class).map(body ->
                                    new LinkedInApiException("Token inválido: " + body)
                            )
                    )
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

    // ── helpers ──────────────────────────────────────────────────────────────

    private void validateCredentials() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new LinkedInApiException(
                    "LINKEDIN_ACCESS_TOKEN no configurado. " +
                    "Agrega la variable de entorno o app.linkedin.access-token en application.yml"
            );
        }
        if (personId == null || personId.isBlank()) {
            throw new LinkedInApiException(
                    "LINKEDIN_PERSON_ID no configurado. " +
                    "Agrega la variable de entorno o app.linkedin.person-id en application.yml"
            );
        }
    }

    /** Excepción específica para errores de la API de LinkedIn */
    public static class LinkedInApiException extends RuntimeException {
        public LinkedInApiException(String message) {
            super(message);
        }
        public LinkedInApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
