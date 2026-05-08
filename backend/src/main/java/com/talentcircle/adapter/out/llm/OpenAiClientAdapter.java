package com.talentcircle.adapter.out.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.port.out.LlmClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador de salida para OpenAI API (GPT-4 / GPT-4o).
 * Marcado como @Primary — se usa cuando LLM_PROVIDER=openai (default).
 *
 * Variables de entorno requeridas:
 *   OPENAI_API_KEY  → sk-...
 *   OPENAI_MODEL    → gpt-4-turbo (default)
 */
@Primary
@Component
public class OpenAiClientAdapter implements LlmClientPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClientAdapter.class);
    private static final String OPENAI_BASE_URL = "https://api.openai.com/v1";

    private final String apiKey;
    private final String model;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public OpenAiClientAdapter(
            @Value("${app.llm.openai.api-key:}") String apiKey,
            @Value("${app.llm.openai.model:gpt-4-turbo}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .baseUrl(OPENAI_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public AiAnalysis analyzeActivity(List<CommunityActivity> activities, String promptTemplate) {
        validateApiKey();

        String activitiesText = buildActivitiesText(activities);
        String prompt = promptTemplate.replace("{activities}", activitiesText);

        log.info("Analizando {} actividades con OpenAI ({})", activities.size(), model);
        String response = callChatCompletions(prompt);

        return parseAnalysisResponse(response, activities.size());
    }

    @Override
    public String generateDraft(String analysisJson, String channel, String promptTemplate) {
        validateApiKey();

        String prompt = promptTemplate
                .replace("{analysis}", analysisJson)
                .replace("{channel}", channel)
                .replace("{ACTIVIDADES}", analysisJson);

        log.info("Generando borrador para canal {} con OpenAI ({})", channel, model);
        return callChatCompletions(prompt);
    }

    @Override
    public boolean validateConnection(String testApiKey) {
        String keyToTest = (testApiKey != null && !testApiKey.isBlank()) ? testApiKey : apiKey;
        if (keyToTest == null || keyToTest.isBlank()) return false;

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 5,
                    "messages", List.of(Map.of("role", "user", "content", "ping"))
            );

            webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + keyToTest)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class)
                            .map(e -> new RuntimeException("OpenAI error: " + e)))
                    .bodyToMono(String.class)
                    .block();

            log.info("Validación de API key OpenAI: OK");
            return true;
        } catch (Exception e) {
            log.warn("API key OpenAI inválida: {}", e.getMessage());
            return false;
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private String callChatCompletions(String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 4096,
                "temperature", 0.7,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "Eres un asistente experto en contenido para comunidades técnicas hispanohablantes."),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String raw = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, r ->
                            r.bodyToMono(String.class).map(e -> {
                                log.error("Error 4xx OpenAI: {}", e);
                                return new RuntimeException("OpenAI API error (4xx): " + e);
                            }))
                    .onStatus(HttpStatusCode::is5xxServerError, r ->
                            r.bodyToMono(String.class).map(e -> {
                                log.error("Error 5xx OpenAI: {}", e);
                                return new RuntimeException("OpenAI API error (5xx): " + e);
                            }))
                    .bodyToMono(String.class)
                    .block();

            // Extraer el texto del campo choices[0].message.content
            JsonNode root = objectMapper.readTree(raw);
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("Error llamando a OpenAI", e);
            throw new RuntimeException("Error al llamar a OpenAI: " + e.getMessage(), e);
        }
    }

    private String buildActivitiesText(List<CommunityActivity> activities) {
        StringBuilder sb = new StringBuilder();
        for (CommunityActivity a : activities) {
            sb.append("- ").append(a.getTitle() != null ? a.getTitle() : "Sin título");
            if (a.getContent() != null) sb.append(": ").append(a.getContent(), 0, Math.min(200, a.getContent().length()));
            sb.append("\n");
        }
        return sb.toString();
    }

    private AiAnalysis parseAnalysisResponse(String content, int activityCount) {
        AiAnalysis analysis = new AiAnalysis();
        analysis.setId(UUID.randomUUID().toString());
        analysis.setExecutiveSummary(content.length() > 500 ? content.substring(0, 500) : content);
        analysis.setRelevanceScores("{}");
        analysis.setTopTopics("[]");
        analysis.setLlmProvider("openai");
        analysis.setPromptTokens(activityCount * 50); // estimado
        analysis.setCompletionTokens(content.length() / 4); // estimado
        return analysis;
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY no configurado. " +
                    "Agrega la variable de entorno o app.llm.openai.api-key en application.yml");
        }
    }
}
