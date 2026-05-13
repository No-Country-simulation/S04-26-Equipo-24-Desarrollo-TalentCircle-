package com.talentcircle.application.service;

import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.CommunityCollectorUseCase;
import com.talentcircle.domain.port.in.AiAnalyzerUseCase;
import com.talentcircle.domain.port.in.DraftGeneratorUseCase;
import com.talentcircle.domain.port.in.PipelineOrchestratorUseCase;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PipelineOrchestratorService implements PipelineOrchestratorUseCase {

    private final WeeklyExecutionRepository executionRepository;
    private final CommunityCollectorUseCase communityCollectorUseCase;
    private final AiAnalyzerUseCase aiAnalyzerUseCase;
    private final DraftGeneratorUseCase draftGeneratorUseCase;

    public PipelineOrchestratorService(
            WeeklyExecutionRepository executionRepository,
            CommunityCollectorUseCase communityCollectorUseCase,
            AiAnalyzerUseCase aiAnalyzerUseCase,
            DraftGeneratorUseCase draftGeneratorUseCase) {
        this.executionRepository = executionRepository;
        this.communityCollectorUseCase = communityCollectorUseCase;
        this.aiAnalyzerUseCase = aiAnalyzerUseCase;
        this.draftGeneratorUseCase = draftGeneratorUseCase;
    }

    @Override
    public String runWeeklyPipeline(String triggeredBy) {
        WeeklyExecution execution = new WeeklyExecution();
        execution.setId(UUID.randomUUID().toString());
        LocalDate now = LocalDate.now();
        execution.setWeekStart(now.minusDays(now.getDayOfWeek().getValue() - 1)); // Monday
        execution.setWeekEnd(execution.getWeekStart().plusDays(6)); // Sunday
        execution.setStatus(WeeklyExecution.ExecutionStatus.RUNNING);
        execution.setStartedAt(LocalDateTime.now());
        execution.setTriggeredBy(triggeredBy);

        execution = executionRepository.save(execution);
        String executionId = execution.getId();

        try {
            // Step 1: Collect activities from all active sources (Discord, Circle, Slack)
            communityCollectorUseCase.collectFromAllActiveSources(executionId);

            // Step 2: Analyze activities with AI
            aiAnalyzerUseCase.analyzeActivity(executionId, "Analyze these activities for content generation");

            // Step 3: Generate drafts for all channels
            draftGeneratorUseCase.generateDrafts(executionId);

            execution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);
            execution.setCompletedAt(LocalDateTime.now());

        } catch (Exception e) {
            execution.setStatus(WeeklyExecution.ExecutionStatus.FAILED);
            execution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(execution);
            throw new RuntimeException("Pipeline failed for execution " + executionId + ": " + e.getMessage(), e);
        }

        executionRepository.save(execution);
        return executionId;
    }

    @Override
    public void retryFailedStep(String executionId) {
        WeeklyExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        if (execution.getStatus() != WeeklyExecution.ExecutionStatus.FAILED) {
            throw new IllegalStateException("Only FAILED executions can be retried");
        }

        // Reset status to RUNNING and retry
        execution.setStatus(WeeklyExecution.ExecutionStatus.RUNNING);
        execution.setCompletedAt(null);
        execution = executionRepository.save(execution);

        try {
            // Retry: recolectar de todas las fuentes activas
            communityCollectorUseCase.collectFromAllActiveSources(executionId);

            // Step 2: Analyze with AI
            aiAnalyzerUseCase.analyzeActivity(executionId, "Analyze these activities for content generation");

            // Step 3: Generate drafts
            draftGeneratorUseCase.generateDrafts(executionId);

            execution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);
            execution.setCompletedAt(LocalDateTime.now());

        } catch (Exception e) {
            execution.setStatus(WeeklyExecution.ExecutionStatus.FAILED);
            execution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(execution);
            throw new RuntimeException("Pipeline retry failed for execution " + executionId + ": " + e.getMessage(), e);
        }

        executionRepository.save(execution);
    }
}
