package com.talentcircle.adapter.out.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.port.out.LlmClientPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Primary
@Component
public class OpenAiClientAdapter implements LlmClientPort {

    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiClientAdapter(
            @Value("${app.llm.openai.api-key:}") String apiKey,
            @Value("${app.llm.openai.model:gpt-4o-mini}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AiAnalysis analyzeActivity(List<CommunityActivity> activities, String promptTemplate) {
        if (apiKey == null || apiKey.isEmpty() || "dev-placeholder".equals(apiKey)) {
            throw new IllegalStateException("OpenAI API key is not configured. Set OPENAI_API_KEY in environment variables.");
        }

        try {
            String prompt = buildAnalysisPrompt(activities, promptTemplate);
            return callOpenAiForAnalysis(prompt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateDraft(String analysisJson, String channel, String promptTemplate) {
        if (apiKey == null || apiKey.isEmpty() || "dev-placeholder".equals(apiKey)) {
            throw new IllegalStateException("OpenAI API key is not configured. Set OPENAI_API_KEY in environment variables.");
        }

        String prompt = String.format("%s\n\nChannel: %s\n\nAnalysis: %s", promptTemplate, channel, analysisJson);

        try {
            return callOpenAiForText(prompt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateConnection(String testApiKey) {
        if (testApiKey == null || testApiKey.isEmpty() || "dev-placeholder".equals(testApiKey)) {
            return false;
        }
        try {
            String response = callOpenAiForText("Hello, respond with 'OK'");
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private AiAnalysis callOpenAiForAnalysis(String prompt) throws Exception {
        Map<String, Object> requestBody = buildChatRequest(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions",
                entity,
                Map.class
        );

        if (response.getBody() != null) {
            String content = extractContentFromResponse(response.getBody());
            AiAnalysis analysis = new AiAnalysis();
            analysis.setId(java.util.UUID.randomUUID().toString());
            analysis.setExecutiveSummary(content);
            analysis.setRelevanceScores("{}");
            analysis.setTopTopics("[]");
            analysis.setLlmProvider("openai");
            analysis.setPromptTokens(0);
            analysis.setCompletionTokens(0);
            return analysis;
        }

        throw new RuntimeException("Empty response from OpenAI API");
    }

    private String callOpenAiForText(String prompt) throws Exception {
        Map<String, Object> requestBody = buildChatRequest(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions",
                entity,
                Map.class
        );

        if (response.getBody() != null) {
            return extractContentFromResponse(response.getBody());
        }

        throw new RuntimeException("Empty response from OpenAI API");
    }

    private Map<String, Object> buildChatRequest(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);

        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a helpful content creator and community manager. Analyze community activities and generate high-quality social media content in SPANISH (Espanol).");
        messages.add(systemMessage);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 4096);
        requestBody.put("temperature", 0.7);

        return requestBody;
    }

    private String buildAnalysisPrompt(List<CommunityActivity> activities, String template) {
        StringBuilder sb = new StringBuilder();
        sb.append(template).append("\n\n");
        sb.append("Here are the community activities to analyze:\n\n");

        for (int i = 0; i < activities.size(); i++) {
            CommunityActivity a = activities.get(i);
            sb.append(i + 1).append(". ");
            if (a.getTitle() != null) sb.append("Title: ").append(a.getTitle()).append("\n");
            if (a.getContent() != null) {
                String contentPreview = a.getContent().substring(0, Math.min(200, a.getContent().length()));
                if (a.getContent().length() > 200) contentPreview += "...";
                sb.append("   Content: ").append(contentPreview).append("\n");
            }
            if (a.getType() != null) sb.append("   Type: ").append(a.getType()).append("\n");
            if (a.getAuthor() != null) sb.append("   Author: ").append(a.getAuthor()).append("\n");
            sb.append("\n");
        }

        return sb.toString();
    }

    private String extractContentFromResponse(Map<String, Object> responseBody) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            if (message != null) {
                return (String) message.get("content");
            }
        }
        return "";
    }
}
