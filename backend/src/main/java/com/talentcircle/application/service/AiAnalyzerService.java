package com.talentcircle.application.service;

import com.talentcircle.adapter.out.llm.LlmClientFactory;
import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.AiAnalyzerUseCase;
import com.talentcircle.domain.port.out.AiAnalysisRepository;
import com.talentcircle.domain.port.out.CommunityActivityRepository;
import com.talentcircle.domain.port.out.LlmClientPort;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AiAnalyzerService implements AiAnalyzerUseCase {

    private static final Logger log = LoggerFactory.getLogger(AiAnalyzerService.class);

    private final WeeklyExecutionRepository executionRepository;
    private final AiAnalysisRepository analysisRepository;
    private final CommunityActivityRepository activityRepository;
    private final LlmClientFactory llmClientFactory;

    public AiAnalyzerService(WeeklyExecutionRepository executionRepository,
                             AiAnalysisRepository analysisRepository,
                             CommunityActivityRepository activityRepository,
                             LlmClientFactory llmClientFactory) {
        this.executionRepository = executionRepository;
        this.analysisRepository  = analysisRepository;
        this.activityRepository  = activityRepository;
        this.llmClientFactory    = llmClientFactory;
    }

    @Override
    public AiAnalysis analyzeActivity(String executionId, String promptTemplate) {
        WeeklyExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId));

        List<CommunityActivity> activities = activityRepository.findByExecutionId(executionId);
        log.info("Analizando {} actividades para ejecución {} con provider={}",
                activities.size(), executionId, llmClientFactory.getClient().getClass().getSimpleName());

        LlmClientPort llm = llmClientFactory.getClient();
        AiAnalysis analysis = llm.analyzeActivity(activities, promptTemplate);

        analysis.setExecution(execution);
        return analysisRepository.save(analysis);
    }
}
