package com.talentcircle.adapter.out.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.port.out.LlmClientPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class OpenAiClientAdapter implements LlmClientPort {

    private final String apiKey;
    private final String model;

    public OpenAiClientAdapter(@Value("${app.llm.openai.api-key:}") String apiKey,
                             @Value("${app.llm.openai.model:gpt-4-turbo}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public AiAnalysis analyzeActivity(List<CommunityActivity> activities, String promptTemplate) {
        // Build prompt with activities
        String prompt = buildAnalysisPrompt(activities, promptTemplate);

        // Call OpenAI API
        String response = callOpenAi(prompt);

        // Parse response and create AiAnalysis
        return parseAnalysisResponse(response);
    }

    @Override
    public String generateDraft(String analysisJson, String channel, String promptTemplate) {
        String prompt = promptTemplate
                .replace("{analysis}", analysisJson)
                .replace("{channel}", channel);

        return callOpenAi(prompt);
    }

    @Override
    public boolean validateConnection(String apiKey) {
        // TODO: Implement with proper HTTP client
        return false;
    }

    private String callOpenAi(String prompt) {
        // Implementation for OpenAI API call
        // POST /chat/completions with JSON body
        throw new RuntimeException("OpenAI adapter not fully implemented yet");
    }

    private String buildAnalysisPrompt(List<CommunityActivity> activities, String template) {
        // Build activities JSON
        return template.replace("{activities}", activities.toString());
    }

    private AiAnalysis parseAnalysisResponse(String response) {
        // Parse OpenAI response
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            // Parse and create AiAnalysis
            AiAnalysis analysis = new AiAnalysis();
            // Set fields from response
            return analysis;
        } catch (Exception e) {
            throw new RuntimeException("Error parsing OpenAI response", e);
        }
    }
}
