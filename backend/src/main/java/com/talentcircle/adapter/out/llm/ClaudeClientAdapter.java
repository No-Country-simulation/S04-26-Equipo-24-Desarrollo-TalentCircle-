package com.talentcircle.adapter.out.llm;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.port.out.LlmClientPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClaudeClientAdapter implements LlmClientPort {

    private final String apiKey;
    private final String model;

    public ClaudeClientAdapter(@Value("${app.llm.claude.api-key:}") String apiKey,
                             @Value("${app.llm.claude.model:claude-3-5-sonnet-20241022}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public AiAnalysis analyzeActivity(List<CommunityActivity> activities, String promptTemplate) {
        // Call Claude API for analysis
        throw new RuntimeException("Claude adapter not fully implemented yet");
    }

    @Override
    public String generateDraft(String analysisJson, String channel, String promptTemplate) {
        // Call Claude API for draft generation
        throw new RuntimeException("Claude adapter not fully implemented yet");
    }

    @Override
    public boolean validateConnection(String apiKey) {
        // TODO: Implement with proper HTTP client
        return false;
    }
}
