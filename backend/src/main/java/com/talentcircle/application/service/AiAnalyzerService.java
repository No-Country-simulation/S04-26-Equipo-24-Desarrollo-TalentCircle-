package com.talentcircle.application.service;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.AiAnalyzerUseCase;
import com.talentcircle.domain.port.out.AiAnalysisRepository;
import com.talentcircle.domain.port.out.CommunityActivityRepository;
import com.talentcircle.domain.port.out.LlmClientPort;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AiAnalyzerService implements AiAnalyzerUseCase {

    private final WeeklyExecutionRepository executionRepository;
    private final AiAnalysisRepository analysisRepository;
    private final CommunityActivityRepository activityRepository;
    private final LlmClientPort llmClient;

    public AiAnalyzerService(WeeklyExecutionRepository executionRepository,
                           AiAnalysisRepository analysisRepository,
                           CommunityActivityRepository activityRepository,
                           LlmClientPort llmClient) {
        this.executionRepository = executionRepository;
        this.analysisRepository = analysisRepository;
        this.activityRepository = activityRepository;
        this.llmClient = llmClient;
    }

    @Override
    public AiAnalysis analyzeActivity(String executionId, String promptTemplate) {
        WeeklyExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found"));

        // Get activities from repository
        List<com.talentcircle.domain.model.CommunityActivity> activities = 
            activityRepository.findByExecutionId(executionId);

        // Call LLM
        AiAnalysis analysis = llmClient.analyzeActivity(activities, promptTemplate);

        // Link to execution
        analysis.setExecution(execution);
        return analysisRepository.save(analysis);
    }
}
