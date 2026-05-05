package com.talentcircle.application.service;

import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.model.CommunitySource;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.CommunityCollectorUseCase;
import com.talentcircle.domain.port.out.CommunityActivityRepository;
import com.talentcircle.domain.port.out.CommunitySourceRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityCollectorServiceTest {

    @Mock
    private CommunityActivityRepository activityRepository;

    @Mock
    private WeeklyExecutionRepository executionRepository;

    @Mock
    private CommunitySourceRepository sourceRepository;

    @InjectMocks
    private CommunityCollectorService collectorService;

    private WeeklyExecution testExecution;
    private CommunitySource testDiscordSource;
    private CommunitySource testCircleSource;
    private CommunitySource testSlackSource;

    @BeforeEach
    void setUp() {
        testExecution = new WeeklyExecution();
        testExecution.setId("exec-123");
        testExecution.setWeekStart(java.time.LocalDate.now());
        testExecution.setWeekEnd(java.time.LocalDate.now().plusDays(7));
        testExecution.setStatus(WeeklyExecution.ExecutionStatus.RUNNING);
        testExecution.setStartedAt(LocalDateTime.now());

        testDiscordSource = new CommunitySource();
        testDiscordSource.setId("source-discord-123");
        testDiscordSource.setName("Discord Server");
        testDiscordSource.setType(CommunitySource.SourceType.DISCORD);
        testDiscordSource.setApiUrl("https://discord.com/api/v10");
        testDiscordSource.setActive(true);

        testCircleSource = new CommunitySource();
        testCircleSource.setId("source-circle-123");
        testCircleSource.setName("Circle Community");
        testCircleSource.setType(CommunitySource.SourceType.CIRCLE);
        testCircleSource.setApiUrl("https://circle.com/api");
        testCircleSource.setActive(true);

        testSlackSource = new CommunitySource();
        testSlackSource.setId("source-slack-123");
        testSlackSource.setName("Slack Workspace");
        testSlackSource.setType(CommunitySource.SourceType.SLACK);
        testSlackSource.setApiUrl("https://slack.com/api");
        testSlackSource.setActive(true);
    }

    @Test
    void collectActivity_shouldCollectFromDiscordSource() {
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(sourceRepository.findById("source-discord-123")).thenReturn(Optional.of(testDiscordSource));
        when(activityRepository.save(any(CommunityActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        collectorService.collectActivity("exec-123", "source-discord-123");

        verify(executionRepository, times(1)).findById("exec-123");
        verify(sourceRepository, times(1)).findById("source-discord-123");
        verify(activityRepository, atLeastOnce()).save(any(CommunityActivity.class));
        verify(executionRepository, times(1)).save(testExecution);

        assertEquals(WeeklyExecution.ExecutionStatus.COMPLETED, testExecution.getStatus());
        assertNotNull(testExecution.getCompletedAt());
    }

    @Test
    void collectActivity_shouldCollectFromCircleSource() {
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(sourceRepository.findById("source-circle-123")).thenReturn(Optional.of(testCircleSource));
        when(activityRepository.save(any(CommunityActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        collectorService.collectActivity("exec-123", "source-circle-123");

        verify(executionRepository, times(1)).findById("exec-123");
        verify(sourceRepository, times(1)).findById("source-circle-123");
        verify(activityRepository, atLeastOnce()).save(any(CommunityActivity.class));
        assertEquals(WeeklyExecution.ExecutionStatus.COMPLETED, testExecution.getStatus());
    }

    @Test
    void collectActivity_shouldCollectFromSlackSource() {
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(sourceRepository.findById("source-slack-123")).thenReturn(Optional.of(testSlackSource));
        when(activityRepository.save(any(CommunityActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        collectorService.collectActivity("exec-123", "source-slack-123");

        verify(executionRepository, times(1)).findById("exec-123");
        verify(sourceRepository, times(1)).findById("source-slack-123");
        verify(activityRepository, atLeastOnce()).save(any(CommunityActivity.class));
        assertEquals(WeeklyExecution.ExecutionStatus.COMPLETED, testExecution.getStatus());
    }

    @Test
    void collectActivity_shouldThrowExceptionWhenExecutionNotFound() {
        when(executionRepository.findById("invalid-exec")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                collectorService.collectActivity("invalid-exec", "source-discord-123")
        );

        assertEquals("Execution not found: invalid-exec", exception.getMessage());
        verify(sourceRepository, never()).findById(anyString());
        verify(activityRepository, never()).save(any(CommunityActivity.class));
    }

    @Test
    void collectActivity_shouldThrowExceptionWhenSourceNotFound() {
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(sourceRepository.findById("invalid-source")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                collectorService.collectActivity("exec-123", "invalid-source")
        );

        assertEquals("Source not found: invalid-source", exception.getMessage());
        verify(activityRepository, never()).save(any(CommunityActivity.class));
    }

    @Test
    void collectActivity_shouldThrowExceptionWhenSourceInactive() {
        testDiscordSource.setActive(false);
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(sourceRepository.findById("source-discord-123")).thenReturn(Optional.of(testDiscordSource));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                collectorService.collectActivity("exec-123", "source-discord-123")
        );

        assertEquals("Source is not active: source-discord-123", exception.getMessage());
        verify(activityRepository, never()).save(any(CommunityActivity.class));
    }

    @Test
    void collectActivity_shouldThrowExceptionForUnsupportedSourceType() {
        CommunitySource unknownSource = new CommunitySource();
        unknownSource.setId("source-unknown-123");
        unknownSource.setName("Unknown Source");
        unknownSource.setType(null);
        unknownSource.setActive(true);

        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(sourceRepository.findById("source-unknown-123")).thenReturn(Optional.of(unknownSource));

        assertThrows(IllegalArgumentException.class, () ->
                collectorService.collectActivity("exec-123", "source-unknown-123")
        );
    }

    @Test
    void getActivitiesByExecution_shouldReturnActivities() {
        CommunityActivity activity1 = new CommunityActivity();
        activity1.setId("act-1");
        activity1.setTitle("Test Activity 1");
        activity1.setType(CommunityActivity.ActivityType.POST);

        CommunityActivity activity2 = new CommunityActivity();
        activity2.setId("act-2");
        activity2.setTitle("Test Activity 2");
        activity2.setType(CommunityActivity.ActivityType.QUESTION);

        List<CommunityActivity> activities = List.of(activity1, activity2);
        when(activityRepository.findByExecutionId("exec-123")).thenReturn(activities);

        List<CommunityCollectorUseCase.CommunityActivityDto> result =
                collectorService.getActivitiesByExecution("exec-123");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Activity 1", result.get(0).title());
        assertEquals("Test Activity 2", result.get(1).title());
        verify(activityRepository, times(1)).findByExecutionId("exec-123");
    }

    @Test
    void getActivitiesByExecution_shouldReturnEmptyListWhenNoActivities() {
        when(activityRepository.findByExecutionId("exec-123")).thenReturn(new ArrayList<>());

        List<CommunityCollectorUseCase.CommunityActivityDto> result =
                collectorService.getActivitiesByExecution("exec-123");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(activityRepository, times(1)).findByExecutionId("exec-123");
    }

    @Test
    void fetchActivities_shouldMapDtoCorrectly() {
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(sourceRepository.findById("source-discord-123")).thenReturn(Optional.of(testDiscordSource));
        when(activityRepository.save(any(CommunityActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        collectorService.collectActivity("exec-123", "source-discord-123");

        verify(activityRepository, atLeastOnce()).save(argThat(activity ->
                activity.getTitle() != null &&
                activity.getContent() != null &&
                activity.getAuthor() != null &&
                activity.getSourceUrl() != null &&
                activity.getPublishedAt() != null &&
                activity.getReactionCount() != null &&
                activity.getResponseCount() != null &&
                activity.getShareCount() != null
        ));
    }
}
