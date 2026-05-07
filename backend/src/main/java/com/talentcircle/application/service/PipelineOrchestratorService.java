package com.talentcircle.application.service;

import com.talentcircle.domain.event.ActivityCollectedEvent;
import com.talentcircle.domain.event.AnalysisCompletedEvent;
import com.talentcircle.domain.event.DraftsGeneratedEvent;
import com.talentcircle.domain.event.PipelineStartedEvent;
import com.talentcircle.domain.model.CommunitySource;
import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.AiAnalyzerUseCase;
import com.talentcircle.domain.port.in.CommunityCollectorUseCase;
import com.talentcircle.domain.port.in.DraftGeneratorUseCase;
import com.talentcircle.domain.port.in.PipelineOrchestratorUseCase;
import com.talentcircle.domain.port.out.CommunityActivityRepository;
import com.talentcircle.domain.port.out.CommunitySourceRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the full weekly pipeline:
 * collect → analyze → generate drafts.
 * Emits domain events at each stage (RF-01, RF-06, RF-09, RF-12).
 */
@Service
@Transactional
public class PipelineOrchestratorService implements PipelineOrchestratorUseCase {

    private static final Logger log = LoggerFactory.getLogger(PipelineOrchestratorService.class);

    private final WeeklyExecutionRepository executionRepository;
    private final CommunitySourceRepository sourceRepository;
    private final CommunityActivityRepository activityRepository;
    private final CommunityCollectorUseCase communityCollectorUseCase;
    private final AiAnalyzerUseCase aiAnalyzerUseCase;
    private final DraftGeneratorUseCase draftGeneratorUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public PipelineOrchestratorService(
            WeeklyExecutionRepository executionRepository,
            CommunitySourceRepository sourceRepository,
            CommunityActivityRepository activityRepository,
            CommunityCollectorUseCase communityCollectorUseCase,
            AiAnalyzerUseCase aiAnalyzerUseCase,
            DraftGeneratorUseCase draftGeneratorUseCase,
            ApplicationEventPublisher eventPublisher) {
        this.executionRepository = executionRepository;
        this.sourceRepository = sourceRepository;
        this.activityRepository = activityRepository;
        this.communityCollectorUseCase = communityCollectorUseCase;
        this.aiAnalyzerUseCase = aiAnalyzerUseCase;
        this.draftGeneratorUseCase = draftGeneratorUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String runWeeklyPipeline(String triggeredBy) {
        // Create execution record
        WeeklyExecution execution = createExecution(triggeredBy);
        String executionId = execution.getId();

        log.info("Pipeline started. executionId={}, triggeredBy={}", executionId, triggeredBy);
        eventPublisher.publishEvent(new PipelineStartedEvent(this, executionId, triggeredBy));

        try {
            // Step 1: Collect from all active sources
            List<CommunitySource> activeSources = sourceRepository.findByActiveTrue();
            if (activeSources.isEmpty()) {
                log.warn("No active community sources found. executionId={}", executionId);
            }

            for (CommunitySource source : activeSources) {
                try {
                    communityCollectorUseCase.collectActivity(executionId, source.getId());
                    log.info("Collected from source={} for executionId={}", source.getName(), executionId);
                } catch (Exception e) {
                    log.error("Failed to collect from source={}: {}", source.getName(), e.getMessage());
                    // Continue with other sources — RF-01 AC4: IF no hay posts, continuar sin fallar
                }
            }

            int itemCount = activityRepository.findByExecutionId(executionId).size();
            eventPublisher.publishEvent(new ActivityCollectedEvent(this, executionId, itemCount));
            log.info("Activity collection complete. executionId={}, itemCount={}", executionId, itemCount);

            // Step 2: Analyze with AI
            var analysis = aiAnalyzerUseCase.analyzeActivity(
                    executionId,
                    "Analiza estas actividades comunitarias y genera un resumen ejecutivo en español con los temas más relevantes."
            );
            eventPublisher.publishEvent(new AnalysisCompletedEvent(this, executionId, analysis.getId()));
            log.info("AI analysis complete. executionId={}, analysisId={}", executionId, analysis.getId());

            // Step 3: Generate drafts for all channels
            List<Draft> drafts = draftGeneratorUseCase.generateDrafts(executionId);
            List<String> draftIds = drafts.stream().map(Draft::getId).toList();
            eventPublisher.publishEvent(new DraftsGeneratedEvent(this, executionId, draftIds));
            log.info("Drafts generated. executionId={}, count={}", executionId, drafts.size());

            // Mark execution as completed
            execution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);
            execution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(execution);

        } catch (Exception e) {
            log.error("Pipeline failed. executionId={}: {}", executionId, e.getMessage(), e);
            execution.setStatus(WeeklyExecution.ExecutionStatus.FAILED);
            execution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(execution);
            throw new RuntimeException("Pipeline failed for execution " + executionId + ": " + e.getMessage(), e);
        }

        return executionId;
    }

    @Override
    public void retryFailedStep(String executionId) {
        WeeklyExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        if (execution.getStatus() != WeeklyExecution.ExecutionStatus.FAILED) {
            throw new IllegalStateException("Only FAILED executions can be retried");
        }

        log.info("Retrying failed pipeline. executionId={}", executionId);
        execution.setStatus(WeeklyExecution.ExecutionStatus.RUNNING);
        execution.setCompletedAt(null);
        executionRepository.save(execution);

        try {
            // Re-run from AI analysis if activities already exist
            int existingActivities = activityRepository.findByExecutionId(executionId).size();
            if (existingActivities == 0) {
                List<CommunitySource> activeSources = sourceRepository.findByActiveTrue();
                for (CommunitySource source : activeSources) {
                    try {
                        communityCollectorUseCase.collectActivity(executionId, source.getId());
                    } catch (Exception e) {
                        log.error("Retry: failed to collect from source={}: {}", source.getName(), e.getMessage());
                    }
                }
            }

            var analysis = aiAnalyzerUseCase.analyzeActivity(executionId,
                    "Analiza estas actividades comunitarias y genera un resumen ejecutivo en español.");
            List<Draft> drafts = draftGeneratorUseCase.generateDrafts(executionId);

            execution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);
            execution.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Retry failed. executionId={}: {}", executionId, e.getMessage(), e);
            execution.setStatus(WeeklyExecution.ExecutionStatus.FAILED);
            execution.setCompletedAt(LocalDateTime.now());
            throw new RuntimeException("Pipeline retry failed for execution " + executionId + ": " + e.getMessage(), e);
        } finally {
            executionRepository.save(execution);
        }
    }

    private WeeklyExecution createExecution(String triggeredBy) {
        WeeklyExecution execution = new WeeklyExecution();
        LocalDate now = LocalDate.now();
        // Week starts on Monday
        execution.setWeekStart(now.minusDays(now.getDayOfWeek().getValue() - 1));
        execution.setWeekEnd(execution.getWeekStart().plusDays(6));
        execution.setStatus(WeeklyExecution.ExecutionStatus.RUNNING);
        execution.setStartedAt(LocalDateTime.now());
        execution.setTriggeredBy(triggeredBy);
        return executionRepository.save(execution);
    }
}
