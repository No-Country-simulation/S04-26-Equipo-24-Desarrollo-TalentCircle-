package com.talentcircle.adapter.out.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.port.out.LlmClientPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ClaudeClientAdapter implements LlmClientPort {

    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ClaudeClientAdapter(@Value("${app.llm.claude.api-key:}") String apiKey,
                             @Value("${app.llm.claude.model:claude-3-5-sonnet-20241022}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restTemplate = createRestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(30).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(60).toMillis());
        return new RestTemplate(factory);
    }

    @Override
    public AiAnalysis analyzeActivity(List<CommunityActivity> activities, String promptTemplate) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Claude API key is not configured");
        }

        try {
            String activitiesJson = objectMapper.writeValueAsString(activities);
            String prompt = String.format("%s\n\nActivities: %s", promptTemplate, activitiesJson);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 4096);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", List.of(message));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.anthropic.com/v1/messages",
                    entity,
                    Map.class
            );

            if (response.getBody() != null) {
                String content = extractContentFromResponse(response.getBody());
                return buildAnalysisWithTokens(parseAnalysisFromJson(content), response.getBody());
            }

            throw new RuntimeException("Empty response from Claude API");

        } catch (Exception e) {
            throw new RuntimeException("Failed to call Claude API: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateDraft(String analysisJson, String channel, String promptTemplate) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Claude API key is not configured");
        }

        try {
            String prompt = String.format("%s\n\nChannel: %s\n\nAnalysis: %s", promptTemplate, channel, analysisJson);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 4096);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", List.of(message));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.anthropic.com/v1/messages",
                    entity,
                    Map.class
            );

            if (response.getBody() != null) {
                return extractContentFromResponse(response.getBody());
            }

            throw new RuntimeException("Empty response from Claude API");

        } catch (Exception e) {
            throw new RuntimeException("Failed to call Claude API: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateConnection(String testApiKey) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 10);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", "Hello");
            requestBody.put("messages", List.of(message));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", testApiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.anthropic.com/v1/messages",
                    entity,
                    Map.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            return false;
        }
    }

    private String extractContentFromResponse(Map<String, Object> responseBody) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
        if (content != null && !content.isEmpty()) {
            Map<String, Object> firstContent = content.get(0);
            return (String) firstContent.get("text");
        }
        return "";
    }

    private AiAnalysis parseAnalysisFromJson(String json) {
        AiAnalysis analysis = new AiAnalysis();
        analysis.setId(java.util.UUID.randomUUID().toString());
        analysis.setExecutiveSummary("Analysis from Claude: " + json.substring(0, Math.min(100, json.length())));
        analysis.setRelevanceScores("{}");
        analysis.setTopTopics("[]");
        analysis.setLlmProvider("claude");
        analysis.setPromptTokens(0);
        analysis.setCompletionTokens(0);
        return analysis;
    }

    private AiAnalysis buildAnalysisWithTokens(AiAnalysis analysis, Map<String, Object> responseBody) {
        try {
            Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
            if (usage != null) {
                if (usage.get("input_tokens") instanceof Number n) {
                    analysis.setPromptTokens(n.intValue());
                }
                if (usage.get("output_tokens") instanceof Number n) {
                    analysis.setCompletionTokens(n.intValue());
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return analysis;
    }
}
