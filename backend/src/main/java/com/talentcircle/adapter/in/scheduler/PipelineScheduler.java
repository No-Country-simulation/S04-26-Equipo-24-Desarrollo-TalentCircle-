package com.talentcircle.adapter.in.scheduler;

import com.talentcircle.domain.port.in.PipelineOrchestratorUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that triggers the weekly pipeline automatically.
 * Default: every Friday at 18:00 (configurable via pipeline_configs.schedule_cron).
 * RF-01: Recolección Automática Semanal.
 */
@Component
@EnableScheduling
public class PipelineScheduler {

    private static final Logger log = LoggerFactory.getLogger(PipelineScheduler.class);

    private final PipelineOrchestratorUseCase pipelineOrchestrator;

    public PipelineScheduler(PipelineOrchestratorUseCase pipelineOrchestrator) {
        this.pipelineOrchestrator = pipelineOrchestrator;
    }

    /**
     * Runs every Friday at 18:00 local time.
     * Cron: second minute hour day-of-month month day-of-week
     * "0 0 18 * * FRI" = at 18:00:00 every Friday
     */
    @Scheduled(cron = "${app.pipeline.schedule-cron:0 0 18 * * FRI}")
    public void runWeeklyPipeline() {
        log.info("Scheduled pipeline triggered — starting weekly execution");
        try {
            String executionId = pipelineOrchestrator.runWeeklyPipeline("SCHEDULER");
            log.info("Scheduled pipeline started successfully. executionId={}", executionId);
        } catch (Exception e) {
            log.error("Scheduled pipeline failed to start: {}", e.getMessage(), e);
        }
    }
}
