package com.talentcircle.application.service;

import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.model.CommunitySource;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.CommunityCollectorUseCase;
import com.talentcircle.domain.port.out.CommunityActivityRepository;
import com.talentcircle.domain.port.out.CommunitySourceRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CommunityCollectorService implements CommunityCollectorUseCase {

    private static final Logger log = LoggerFactory.getLogger(CommunityCollectorService.class);

    private final CommunityActivityRepository activityRepository;
    private final WeeklyExecutionRepository executionRepository;
    private final CommunitySourceRepository sourceRepository;
    private final DiscordCollectorService discordCollectorService;

    public CommunityCollectorService(CommunityActivityRepository activityRepository,
                                     WeeklyExecutionRepository executionRepository,
                                     CommunitySourceRepository sourceRepository,
                                     DiscordCollectorService discordCollectorService) {
        this.activityRepository = activityRepository;
        this.executionRepository = executionRepository;
        this.sourceRepository = sourceRepository;
        this.discordCollectorService = discordCollectorService;
    }

    @Override
    public void collectActivity(String executionId, String sourceId) {
        WeeklyExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        CommunitySource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source not found: " + sourceId));

        if (!source.isActive()) {
            throw new IllegalStateException("Source is not active: " + sourceId);
        }

        log.info("Recolectando actividad de fuente '{}' (tipo: {})", source.getName(), source.getType());

        List<CommunityActivity> activities = fetchActivitiesFromSource(source, execution);

        log.info("Recolectadas {} actividades de '{}'", activities.size(), source.getName());

        execution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);
        execution.setCompletedAt(LocalDateTime.now());
        executionRepository.save(execution);
    }

    /**
     * Recolecta de todas las fuentes activas de Discord asociadas a una ejecución.
     * Llamado por el PipelineOrchestratorService.
     */
    public void collectFromAllActiveSources(String executionId) {
        WeeklyExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        List<CommunitySource> activeSources = sourceRepository.findAllByActiveTrue();

        if (activeSources.isEmpty()) {
            log.warn("No hay fuentes activas configuradas para la ejecución {}", executionId);
            return;
        }

        for (CommunitySource source : activeSources) {
            try {
                fetchActivitiesFromSource(source, execution);
            } catch (Exception e) {
                log.error("Error recolectando de fuente '{}': {}", source.getName(), e.getMessage(), e);
            }
        }

        execution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);
        execution.setCompletedAt(LocalDateTime.now());
        executionRepository.save(execution);
    }

    @Override
    public List<CommunityActivityDto> getActivitiesByExecution(String executionId) {
        return activityRepository.findByExecutionId(executionId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    // ── Dispatch por tipo de fuente ───────────────────────────────────────────

    private List<CommunityActivity> fetchActivitiesFromSource(CommunitySource source,
                                                               WeeklyExecution execution) {
        if (source.getType() == null) {
            throw new IllegalArgumentException("Source type is null for source: " + source.getId());
        }

        return switch (source.getType()) {
            case DISCORD -> discordCollectorService.collectWeeklyActivities(source, execution);
            case CIRCLE  -> fetchCircleActivities(source, execution);
            case SLACK   -> fetchSlackActivities(source, execution);
        };
    }

    // ── Stubs para Circle y Slack (a implementar en futuras iteraciones) ──────

    private List<CommunityActivity> fetchCircleActivities(CommunitySource source,
                                                           WeeklyExecution execution) {
        log.info("Circle collector no implementado aún para fuente '{}'", source.getName());
        return List.of();
    }

    private List<CommunityActivity> fetchSlackActivities(CommunitySource source,
                                                          WeeklyExecution execution) {
        log.info("Slack collector no implementado aún para fuente '{}'", source.getName());
        return List.of();
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private CommunityActivityDto mapToDto(CommunityActivity activity) {
        return new CommunityActivityDto(
                activity.getId(),
                activity.getTitle(),
                activity.getContent(),
                activity.getType() != null ? activity.getType().name() : null,
                activity.getReactionCount(),
                activity.getResponseCount(),
                activity.getShareCount(),
                activity.getAuthor(),
                activity.getSourceUrl()
        );
    }
}
