package com.talentcircle.adapter.out.llm;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.port.out.LlmClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LLM adapter that routes requests through OpenRouter (https://openrouter.ai).
 *
 * <p>OpenRouter exposes an OpenAI-compatible {@code /api/v1/chat/completions}
 * endpoint that gives access to 300+ models (GPT-4o, Claude, Gemini, Llama,
 * DeepSeek, Mistral, etc.) through a single API key.
 *
 * <p>To activate this adapter instead of OpenAI, set:
 * <pre>
 *   app.llm.provider=openrouter          # in application.yml / env var LLM_PROVIDER
 *   app.llm.openrouter.api-key=sk-or-... # OPENROUTER_API_KEY env var
 *   app.llm.openrouter.model=...         # e.g. google/gemini-flash-1.5
 * </pre>
 *
 * @see <a href="https://openrouter.ai/docs/quickstart">OpenRouter Quickstart</a>
 */
@Component("openRouterClient")
public class OpenRouterClientAdapter implements LlmClientPort {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterClientAdapter.class);

    private static final String BASE_URL  = "https://openrouter.ai/api/v1/chat/completions";
    private static final String SITE_URL  = "https://talentcircle.app";
    private static final String SITE_NAME = "TalentCircle";

    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate;

    public OpenRouterClientAdapter(
            @Value("${app.llm.openrouter.api-key:}") String apiKey,
            @Value("${app.llm.openrouter.model:google/gemini-flash-1.5}") String model) {
        this.apiKey = apiKey;
        this.model  = model;
        // Forzar IPv4 — evita UnknownHostException en redes sin conectividad IPv6
        System.setProperty("java.net.preferIPv4Stack", "true");
        this.restTemplate = new RestTemplate();
    }

    // ── LlmClientPort ─────────────────────────────────────────────────────────

    @Override
    public AiAnalysis analyzeActivity(List<CommunityActivity> activities, String promptTemplate) {
        validateApiKey();
        log.info("OpenRouter analyzeActivity — model={}, activities={}", model, activities.size());

        try {
            String prompt   = buildAnalysisPrompt(activities, promptTemplate);
            String content  = callApi(prompt);

            AiAnalysis analysis = new AiAnalysis();
            analysis.setId(UUID.randomUUID().toString());
            analysis.setExecutiveSummary(content);
            analysis.setTopTopics("[]");
            analysis.setRelevanceScores("{}");
            analysis.setLlmProvider("openrouter/" + model);
            analysis.setPromptTokens(0);
            analysis.setCompletionTokens(0);
            return analysis;

        } catch (Exception e) {
            throw new RuntimeException("OpenRouter analyzeActivity failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateDraft(String analysisJson, String channel, String promptTemplate) {
        validateApiKey();
        log.info("OpenRouter generateDraft — model={}, channel={}", model, channel);

        try {
            String prompt = String.format(
                    "%s\n\nCanal de publicación: %s\n\nAnálisis de la comunidad:\n%s",
                    promptTemplate, channel, analysisJson);
            return callApi(prompt);

        } catch (Exception e) {
            throw new RuntimeException("OpenRouter generateDraft failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateConnection(String testApiKey) {
        String keyToTest = (testApiKey != null && !testApiKey.isBlank()) ? testApiKey : this.apiKey;
        if (keyToTest == null || keyToTest.isBlank()) return false;

        try {
            callApiWithKey("Respond with the single word: OK", keyToTest);
            return true;
        } catch (Exception e) {
            log.warn("OpenRouter connection validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OpenRouter API key is not configured. " +
                    "Set OPENROUTER_API_KEY environment variable or app.llm.openrouter.api-key in application.yml.");
        }
    }

    private String callApi(String userPrompt) {
        return callApiWithKey(userPrompt, this.apiKey);
    }

    private String callApiWithKey(String userPrompt, String key) {
        HttpHeaders headers = buildHeaders(key);
        Map<String, Object> body = buildRequestBody(userPrompt);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(BASE_URL, entity, Map.class);
            if (response.getBody() == null) {
                throw new RuntimeException("Empty response body from OpenRouter");
            }
            return extractContent(response.getBody());

        } catch (HttpClientErrorException e) {
            String detail = e.getResponseBodyAsString();
            log.error("OpenRouter HTTP {} — {}", e.getStatusCode(), detail);
            throw new RuntimeException(
                    "OpenRouter returned " + e.getStatusCode() + ": " + detail, e);
        }
    }

    /**
     * Builds the HTTP headers required by OpenRouter.
     * {@code HTTP-Referer} and {@code X-Title} are optional but recommended
     * so the app appears in the OpenRouter usage dashboard.
     */
    private HttpHeaders buildHeaders(String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(key);
        headers.set("HTTP-Referer", SITE_URL);
        headers.set("X-Title",      SITE_NAME);
        return headers;
    }

    /**
     * Builds the OpenAI-compatible chat completions request body.
     * The {@code model} field accepts any OpenRouter model slug
     * (e.g. {@code google/gemini-flash-1.5}, {@code anthropic/claude-3.5-sonnet},
     * {@code meta-llama/llama-3.1-8b-instruct:free}).
     */
    private Map<String, Object> buildRequestBody(String userPrompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 4096);
        body.put("temperature", 0.7);

        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> system = new HashMap<>();
        system.put("role", "system");
        system.put("content",
                "Eres un experto en gestión de comunidades y creación de contenido. " +
                "Analiza las actividades de la comunidad TalentCircle y genera contenido " +
                "de alta calidad en español para los canales indicados.");
        messages.add(system);

        Map<String, Object> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", userPrompt);
        messages.add(user);

        body.put("messages", messages);
        return body;
    }

    private String buildAnalysisPrompt(List<CommunityActivity> activities, String template) {
        StringBuilder sb = new StringBuilder(template).append("\n\n");
        sb.append("Actividades de la comunidad a analizar:\n\n");

        for (int i = 0; i < activities.size(); i++) {
            CommunityActivity a = activities.get(i);
            sb.append(i + 1).append(". ");
            if (a.getTitle()   != null) sb.append("Título: ").append(a.getTitle()).append("\n");
            if (a.getContent() != null) {
                String preview = a.getContent().substring(0, Math.min(200, a.getContent().length()));
                if (a.getContent().length() > 200) preview += "…";
                sb.append("   Contenido: ").append(preview).append("\n");
            }
            if (a.getType()   != null) sb.append("   Tipo: ").append(a.getType()).append("\n");
            if (a.getAuthor() != null) sb.append("   Autor: ").append(a.getAuthor()).append("\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    /** Extracts the assistant message content from an OpenAI-compatible response. */
    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> body) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("OpenRouter response has no choices");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            throw new RuntimeException("OpenRouter choice has no message");
        }
        String content = (String) message.get("content");
        return content != null ? content : "";
    }
}
