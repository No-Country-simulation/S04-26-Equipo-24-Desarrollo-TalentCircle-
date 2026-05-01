package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunityActivity;

import java.util.List;

public interface LlmClientPort {
    AiAnalysis analyzeActivity(List<CommunityActivity> activities, String promptTemplate);
    String generateDraft(String analysisJson, String channel, String promptTemplate);
    boolean validateConnection(String apiKey);
}
