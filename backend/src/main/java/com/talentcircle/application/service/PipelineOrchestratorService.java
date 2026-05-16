package com.talentcircle.application.service;

import com.talentcircle.domain.model.PipelineConfig;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.CommunityCollectorUseCase;
import com.talentcircle.domain.port.in.AiAnalyzerUseCase;
import com.talentcircle.domain.port.in.DraftGeneratorUseCase;
import com.talentcircle.domain.port.in.PipelineOrchestratorUseCase;
import com.talentcircle.domain.port.out.PipelineConfigRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PipelineOrchestratorService implements PipelineOrchestratorUseCase {

    private static final Logger log = LoggerFactory.getLogger(PipelineOrchestratorService.class);

    private static final String DEFAULT_ANALYSIS_PROMPT =
            "Analiza las siguientes actividades de la comunidad TalentCircle de esta semana. " +
            "Identifica los temas más relevantes, las preguntas frecuentes, los recursos compartidos " +
            "y el nivel de engagement. Genera un resumen ejecutivo en español que sirva de base " +
            "para crear contenido para newsletter, LinkedIn y Twitter.";

    private final WeeklyExecutionRepository executionRepository;
    private final CommunityCollectorUseCase communityCollectorUseCase;
    private final AiAnalyzerUseCase aiAnalyzerUseCase;
    private final DraftGeneratorUseCase draftGeneratorUseCase;
    private final PipelineConfigRepository configRepository;

    public PipelineOrchestratorService(
            WeeklyExecutionRepository executionRepository,
            CommunityCollectorUseCase communityCollectorUseCase,
            AiAnalyzerUseCase aiAnalyzerUseCase,
            DraftGeneratorUseCase draftGeneratorUseCase,
            PipelineConfigRepository configRepository) {
        this.executionRepository = executionRepository;
        this.communityCollectorUseCase = communityCollectorUseCase;
        this.aiAnalyzerUseCase = aiAnalyzerUseCase;
        this.draftGeneratorUseCase = draftGeneratorUseCase;
        this.configRepository = configRepository;
    }

    @Override
    public String runWeeklyPipeline(String triggeredBy) {
        WeeklyExecution execution = new WeeklyExecution();
        execution.setId(UUID.randomUUID().toString());
        LocalDate now = LocalDate.now();
        execution.setWeekStart(now.minusDays(now.getDayOfWeek().getValue() - 1));
        execution.setWeekEnd(execution.getWeekStart().plusDays(6));
        execution.setStatus(WeeklyExecution.ExecutionStatus.RUNNING);
        execution.setStartedAt(LocalDateTime.now());
        execution.setTriggeredBy(triggeredBy);

        execution = executionRepository.save(execution);
        String executionId = execution.getId();

        log.info("Pipeline iniciado — executionId={}, triggeredBy={}", executionId, triggeredBy);

        try {
            // Step 1: Recolectar actividades de todas las fuentes activas (Discord, Circle, Slack)
            log.info("[{}] Step 1/3 — Recolectando actividades de la comunidad", executionId);
            communityCollectorUseCase.collectFromAllActiveSources(executionId);

            // Step 2: Analizar actividades con IA usando el prompt configurado en BD
            log.info("[{}] Step 2/3 — Analizando actividades con IA", executionId);
            String analysisPrompt = resolveAnalysisPrompt();
            aiAnalyzerUseCase.analyzeActivity(executionId, analysisPrompt);

            // Step 3: Generar borradores para todos los canales con prompts de BD
            log.info("[{}] Step 3/3 — Generando borradores (Newsletter, LinkedIn, Twitter)", executionId);
            draftGeneratorUseCase.generateDrafts(executionId);

            execution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);
            execution.setCompletedAt(LocalDateTime.now());
            log.info("[{}] Pipeline completado exitosamente", executionId);

        } catch (Exception e) {
            execution.setStatus(WeeklyExecution.ExecutionStatus.FAILED);
            execution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(execution);
            log.error("[{}] Pipeline fallido: {}", executionId, e.getMessage(), e);
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

        execution.setStatus(WeeklyExecution.ExecutionStatus.RUNNING);
        execution.setCompletedAt(null);
        execution = executionRepository.save(execution);

        log.info("[{}] Reintentando pipeline fallido", executionId);

        try {
            communityCollectorUseCase.collectFromAllActiveSources(executionId);

            String analysisPrompt = resolveAnalysisPrompt();
            aiAnalyzerUseCase.analyzeActivity(executionId, analysisPrompt);

            draftGeneratorUseCase.generateDrafts(executionId);

            execution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);
            execution.setCompletedAt(LocalDateTime.now());
            log.info("[{}] Reintento completado exitosamente", executionId);

        } catch (Exception e) {
            execution.setStatus(WeeklyExecution.ExecutionStatus.FAILED);
            execution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(execution);
            log.error("[{}] Reintento fallido: {}", executionId, e.getMessage(), e);
            throw new RuntimeException("Pipeline retry failed for execution " + executionId + ": " + e.getMessage(), e);
        }

        executionRepository.save(execution);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Lee el prompt de análisis desde la configuración en BD.
     * Si no hay configuración o el campo está vacío, usa el prompt por defecto.
     * El campo reutilizado es {@code newsletterPrompt} como base de análisis,
     * o se puede añadir un campo dedicado en {@link PipelineConfig} en el futuro.
     */
    private String resolveAnalysisPrompt() {
        return configRepository.findSingleton()
                .map(PipelineConfig::getNewsletterPrompt)
                .filter(p -> p != null && !p.isBlank())
                .map(p -> DEFAULT_ANALYSIS_PROMPT + "\n\nContexto adicional del administrador: " + p)
                .orElse(DEFAULT_ANALYSIS_PROMPT);
    }
}
