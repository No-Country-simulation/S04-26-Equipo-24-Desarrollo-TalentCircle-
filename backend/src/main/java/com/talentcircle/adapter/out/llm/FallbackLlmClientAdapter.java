package com.talentcircle.adapter.out.llm;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.port.out.LlmClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class FallbackLlmClientAdapter implements LlmClientPort {

    private static final Logger log = LoggerFactory.getLogger(FallbackLlmClientAdapter.class);

    private final OpenAiClientAdapter openAiAdapter;
    private final ClaudeClientAdapter claudeAdapter;
    private final String primaryProvider;

    public FallbackLlmClientAdapter(OpenAiClientAdapter openAiAdapter,
                                    ClaudeClientAdapter claudeAdapter,
                                    @Value("${app.llm.provider:openai}") String primaryProvider) {
        this.openAiAdapter = openAiAdapter;
        this.claudeAdapter = claudeAdapter;
        this.primaryProvider = primaryProvider;
    }

    @Override
    public AiAnalysis analyzeActivity(List<CommunityActivity> activities, String promptTemplate) {
        try {
            return getPrimary().analyzeActivity(activities, promptTemplate);
        } catch (Exception e) {
            log.warn("Primary LLM ({}) failed in analyzeActivity, falling back to secondary: {}",
                    primaryProvider, e.getMessage());
            return getSecondary().analyzeActivity(activities, promptTemplate);
        }
    }

    @Override
    public String generateDraft(String analysisJson, String channel, String promptTemplate) {
        try {
            return getPrimary().generateDraft(analysisJson, channel, promptTemplate);
        } catch (Exception e) {
            log.warn("Primary LLM ({}) failed in generateDraft, falling back to secondary: {}",
                    primaryProvider, e.getMessage());
            return getSecondary().generateDraft(analysisJson, channel, promptTemplate);
        }
    }

    @Override
    public boolean validateConnection(String apiKey) {
        try {
            return getPrimary().validateConnection(apiKey);
        } catch (Exception e) {
            log.warn("Primary LLM ({}) failed in validateConnection, trying secondary: {}",
                    primaryProvider, e.getMessage());
            try {
                return getSecondary().validateConnection(apiKey);
            } catch (Exception e2) {
                log.error("Secondary LLM ({}) also failed in validateConnection: {}",
                        getSecondaryProviderName(), e2.getMessage());
                return false;
            }
        }
    }

    private String getSecondaryProviderName() {
        return "anthropic".equalsIgnoreCase(primaryProvider) ? "openai" : "anthropic";
    }

    private LlmClientPort getPrimary() {
        return "anthropic".equalsIgnoreCase(primaryProvider) ? claudeAdapter : openAiAdapter;
    }

    private LlmClientPort getSecondary() {
        return "anthropic".equalsIgnoreCase(primaryProvider) ? openAiAdapter : claudeAdapter;
    }
}
