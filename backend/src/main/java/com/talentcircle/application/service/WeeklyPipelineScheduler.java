package com.talentcircle.application.service;

import com.talentcircle.domain.port.in.PipelineOrchestratorUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler que dispara el pipeline completo automáticamente
 * todos los viernes a las 7:00 AM (zona horaria del servidor).
 *
 * Cron: "0 0 7 * * FRI"
 *   └─ segundos minutos horas día-mes mes día-semana
 */
@Component
public class WeeklyPipelineScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyPipelineScheduler.class);

    private final PipelineOrchestratorUseCase pipelineOrchestrator;

    public WeeklyPipelineScheduler(PipelineOrchestratorUseCase pipelineOrchestrator) {
        this.pipelineOrchestrator = pipelineOrchestrator;
    }

    /**
     * Ejecución automática: todos los viernes a las 7:00 AM.
     * Recolecta actividad Discord lunes-viernes, analiza con IA y genera borradores.
     */
    @Scheduled(cron = "0 0 7 * * FRI", zone = "America/Bogota")
    public void runWeeklyPipeline() {
        log.info("⏰ Iniciando pipeline semanal automático (viernes 7:00 AM)");
        try {
            String executionId = pipelineOrchestrator.runWeeklyPipeline("scheduler");
            log.info("✅ Pipeline semanal completado. ExecutionId: {}", executionId);
        } catch (Exception e) {
            log.error("❌ Error en pipeline semanal automático: {}", e.getMessage(), e);
        }
    }
}
