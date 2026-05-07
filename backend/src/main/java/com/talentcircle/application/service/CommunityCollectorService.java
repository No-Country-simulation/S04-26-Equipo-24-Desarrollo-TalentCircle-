package com.talentcircle.application.service;

import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.model.CommunitySource;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.CommunityCollectorUseCase;
import com.talentcircle.domain.port.out.CommunityActivityRepository;
import com.talentcircle.domain.port.out.CommunityClientPort;
import com.talentcircle.domain.port.out.CommunitySourceRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects community activity from configured sources.
 * RF-01 to RF-06: Recolección de Actividad Comunitaria.
 * Persists all raw activity before AI processing (RF-06).
 */
@Service
@Transactional
public class CommunityCollectorService implements CommunityCollectorUseCase {

    private static final Logger log = LoggerFactory.getLogger(CommunityCollectorService.class);
    private static final int DEFAULT_ITEMS_PER_TYPE = 10;

    private final CommunityActivityRepository activityRepository;
    private final WeeklyExecutionRepository executionRepository;
    private final CommunitySourceRepository sourceRepository;
    private final CommunityClientPort discordClient;
    private final CommunityClientPort circleClient;
    private final CommunityClientPort slackClient;

    public CommunityCollectorService(
            CommunityActivityRepository activityRepository,
            WeeklyExecutionRepository executionRepository,
            CommunitySourceRepository sourceRepository,
            @Qualifier("discordClientAdapter") CommunityClientPort discordClient,
            @Qualifier("circleClientAdapter") CommunityClientPort circleClient,
            @Qualifier("slackClientAdapter") CommunityClientPort slackClient) {
        this.activityRepository = activityRepository;
        this.executionRepository = executionRepository;
        this.sourceRepository = sourceRepository;
        this.discordClient = discordClient;
        this.circleClient = circleClient;
        this.slackClient = slackClient;
    }

    @Override
    public void collectActivity(String executionId, String sourceId) {
        WeeklyExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        CommunitySource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source not found: " + sourceId));

        if (!source.isActive()) {
            log.warn("Skipping inactive source: {}", sourceId);
            return;
        }

        log.info("Collecting from source={} type={} for executionId={}", source.getName(), source.getType(), executionId);

        CommunityClientPort client = resolveClient(source.getType());
        String apiUrl = source.getApiUrl();
        String apiKey = source.getApiKeyEncrypted(); // already decrypted by AdminService before storing in memory

        List<CommunityActivity> activities = new ArrayList<>();

        // RF-02: Posts más reaccionados
        try {
            activities.addAll(client.fetchTopPosts(apiUrl, apiKey, DEFAULT_ITEMS_PER_TYPE));
        } catch (Exception e) {
            log.error("Failed to fetch posts from {}: {}", source.getName(), e.getMessage());
        }

        // RF-03: Preguntas más respondidas
        try {
            activities.addAll(client.fetchTopQuestions(apiUrl, apiKey, DEFAULT_ITEMS_PER_TYPE));
        } catch (Exception e) {
            log.error("Failed to fetch questions from {}: {}", source.getName(), e.getMessage());
        }

        // RF-04: Recursos más compartidos
        try {
            activities.addAll(client.fetchTopResources(apiUrl, apiKey, DEFAULT_ITEMS_PER_TYPE));
        } catch (Exception e) {
            log.error("Failed to fetch resources from {}: {}", source.getName(), e.getMessage());
        }

        // RF-06: Persist ALL raw activity BEFORE AI processing
        for (CommunityActivity activity : activities) {
            activity.setExecution(execution);
            activity.setSourceId(source.getId());
            activityRepository.save(activity);
        }

        log.info("Persisted {} activities from source={} for executionId={}", activities.size(), source.getName(), executionId);
    }

    @Override
    public List<CommunityActivityDto> getActivitiesByExecution(String executionId) {
        return activityRepository.findByExecutionId(executionId).stream()
                .map(this::mapToDto)
                .toList();
    }

    private CommunityClientPort resolveClient(CommunitySource.SourceType type) {
        return switch (type) {
            case DISCORD -> discordClient;
            case CIRCLE -> circleClient;
            case SLACK -> slackClient;
        };
    }

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
