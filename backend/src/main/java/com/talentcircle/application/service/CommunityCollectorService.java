package com.talentcircle.application.service;

import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.model.CommunitySource;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.CommunityCollectorUseCase;
import com.talentcircle.domain.port.out.CommunityActivityRepository;
import com.talentcircle.domain.port.out.CommunitySourceRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CommunityCollectorService implements CommunityCollectorUseCase {

    private final CommunityActivityRepository activityRepository;
    private final WeeklyExecutionRepository executionRepository;
    private final CommunitySourceRepository sourceRepository;

    public CommunityCollectorService(CommunityActivityRepository activityRepository,
                                   WeeklyExecutionRepository executionRepository,
                                   CommunitySourceRepository sourceRepository) {
        this.activityRepository = activityRepository;
        this.executionRepository = executionRepository;
        this.sourceRepository = sourceRepository;
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

        List<CommunityActivity> activities = fetchActivitiesFromSource(source, execution);
        activities.forEach(activity -> {
            activity.setExecution(execution);
            activityRepository.save(activity);
        });

        execution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);
        execution.setCompletedAt(LocalDateTime.now());
        executionRepository.save(execution);
    }

    @Override
    public List<CommunityActivityDto> getActivitiesByExecution(String executionId) {
        List<CommunityActivity> activities = activityRepository.findByExecutionId(executionId);
        return activities.stream()
                .map(this::mapToDto)
                .toList();
    }

    private List<CommunityActivity> fetchActivitiesFromSource(CommunitySource source, WeeklyExecution execution) {
        List<CommunityActivity> activities = new ArrayList<>();

        CommunitySource.SourceType sourceType = source.getType();
        if (sourceType == null) {
            throw new IllegalArgumentException("Source type is null for source: " + source.getId());
        }

        switch (sourceType) {
            case DISCORD:
                activities.addAll(fetchDiscordActivities(source));
                break;
            case CIRCLE:
                activities.addAll(fetchCircleActivities(source));
                break;
            case SLACK:
                activities.addAll(fetchSlackActivities(source));
                break;
            default:
                throw new IllegalArgumentException("Unsupported source type: " + sourceType);
        }

        return activities;
    }

    private List<CommunityActivity> fetchDiscordActivities(CommunitySource source) {
        List<CommunityActivity> activities = new ArrayList<>();
        // Simulate Discord API call
        CommunityActivity activity = new CommunityActivity();
        activity.setId(UUID.randomUUID().toString());
        activity.setType(CommunityActivity.ActivityType.POST);
        activity.setTitle("Discord Community Update");
        activity.setContent("Sample Discord post from " + source.getName());
        activity.setAuthor("Discord User");
        activity.setPublishedAt(LocalDateTime.now());
        activity.setSourceUrl(source.getApiUrl() + "/channels/123/messages/456");
        activity.setReactionCount(10);
        activity.setResponseCount(5);
        activity.setShareCount(2);
        activities.add(activity);
        return activities;
    }

    private List<CommunityActivity> fetchCircleActivities(CommunitySource source) {
        List<CommunityActivity> activities = new ArrayList<>();
        // Simulate Circle API call
        CommunityActivity activity = new CommunityActivity();
        activity.setId(UUID.randomUUID().toString());
        activity.setType(CommunityActivity.ActivityType.QUESTION);
        activity.setTitle("Circle Community Question");
        activity.setContent("Sample Circle question from " + source.getName());
        activity.setAuthor("Circle Member");
        activity.setPublishedAt(LocalDateTime.now());
        activity.setSourceUrl(source.getApiUrl() + "/posts/789");
        activity.setReactionCount(15);
        activity.setResponseCount(8);
        activity.setShareCount(3);
        activities.add(activity);
        return activities;
    }

    private List<CommunityActivity> fetchSlackActivities(CommunitySource source) {
        List<CommunityActivity> activities = new ArrayList<>();
        // Simulate Slack API call
        CommunityActivity activity = new CommunityActivity();
        activity.setId(UUID.randomUUID().toString());
        activity.setType(CommunityActivity.ActivityType.RESOURCE);
        activity.setTitle("Slack Resource Share");
        activity.setContent("Sample Slack resource from " + source.getName());
        activity.setAuthor("Slack User");
        activity.setPublishedAt(LocalDateTime.now());
        activity.setSourceUrl(source.getApiUrl() + "/messages/012");
        activity.setReactionCount(20);
        activity.setResponseCount(12);
        activity.setShareCount(5);
        activities.add(activity);
        return activities;
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
