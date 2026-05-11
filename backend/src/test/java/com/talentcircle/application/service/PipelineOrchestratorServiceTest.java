package com.talentcircle.application.service;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.model.CommunitySource;
import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.AiAnalyzerUseCase;
import com.talentcircle.domain.port.in.CommunityCollectorUseCase;
import com.talentcircle.domain.port.in.DraftGeneratorUseCase;
import com.talentcircle.domain.port.out.CommunityActivityRepository;
import com.talentcircle.domain.port.out.CommunitySourceRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for PipelineOrchestratorService.
 * Tarea 15.2: Tests de integración para el pipeline completo.
 */
@ExtendWith(MockitoExtension.class)
class PipelineOrchestratorServiceTest {

    @Mock private WeeklyExecutionRepository executionRepository;
    @Mock private CommunitySourceRepository sourceRepository;
    @Mock private CommunityActivityRepository activityRepository;
    @Mock private CommunityCollectorUseCase communityCollector;
    @Mock private AiAnalyzerUseCase aiAnalyzer;
    @Mock private DraftGeneratorUseCase draftGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;

    private PipelineOrchestratorService orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new PipelineOrchestratorService(
                executionRepository, sourceRepository, activityRepository,
                communityCollector, aiAnalyzer, draftGenerator, eventPublisher);
    }

    @Test
    void runWeeklyPipeline_withActiveSources_completesSuccessfully() {
        // Setup
        WeeklyExecution execution = buildExecution("exec-1");
        CommunitySource source = buildSource("src-1", "Discord Test");
        AiAnalysis analysis = buildAnalysis("analysis-1");
        Draft draft = buildDraft("draft-1", Draft.Channel.LINKEDIN);

        when(executionRepository.save(any())).thenReturn(execution);
        when(sourceRepository.findByActiveTrue()).thenReturn(List.of(source));
        when(activityRepository.findByExecutionId("exec-1")).thenReturn(List.of(new CommunityActivity()));
        when(aiAnalyzer.analyzeActivity(anyString(), anyString())).thenReturn(analysis);
        when(draftGenerator.generateDrafts("exec-1")).thenReturn(List.of(draft));
        doNothing().when(communityCollector).collectActivity(anyString(), anyString());

        // Execute
        String executionId = orchestrator.runWeeklyPipeline("ADMIN");

        // Verify
        assertNotNull(executionId);
        verify(communityCollector).collectActivity("exec-1", "src-1");
        verify(aiAnalyzer).analyzeActivity(eq("exec-1"), anyString());
        verify(draftGenerator).generateDrafts("exec-1");
        // Verify events were published
        verify(eventPublisher, times(3)).publishEvent(any()); // Started + Collected + Analysis + Drafts = 4 but Started is first
    }

    @Test
    void runWeeklyPipeline_withNoActiveSources_continuesWithoutFailing() {
        WeeklyExecution execution = buildExecution("exec-2");
        AiAnalysis analysis = buildAnalysis("analysis-2");

        when(executionRepository.save(any())).thenReturn(execution);
        when(sourceRepository.findByActiveTrue()).thenReturn(List.of());
        when(activityRepository.findByExecutionId("exec-2")).thenReturn(List.of());
        when(aiAnalyzer.analyzeActivity(anyString(), anyString())).thenReturn(analysis);
        when(draftGenerator.generateDrafts("exec-2")).thenReturn(List.of());

        // Should not throw even with no sources
        assertDoesNotThrow(() -> orchestrator.runWeeklyPipeline("SCHEDULER"));
        verify(communityCollector, never()).collectActivity(anyString(), anyString());
    }

    @Test
    void runWeeklyPipeline_whenAiFails_marksExecutionAsFailed() {
        WeeklyExecution execution = buildExecution("exec-3");
        CommunitySource source = buildSource("src-1", "Discord");

        when(executionRepository.save(any())).thenReturn(execution);
        when(sourceRepository.findByActiveTrue()).thenReturn(List.of(source));
        when(activityRepository.findByExecutionId("exec-3")).thenReturn(List.of());
        doNothing().when(communityCollector).collectActivity(anyString(), anyString());
        when(aiAnalyzer.analyzeActivity(anyString(), anyString()))
                .thenThrow(new RuntimeException("LLM API unavailable"));

        assertThrows(RuntimeException.class, () -> orchestrator.runWeeklyPipeline("ADMIN"));
        // Verify execution was saved with FAILED status
        verify(executionRepository, atLeastOnce()).save(argThat(e ->
                e.getStatus() == WeeklyExecution.ExecutionStatus.FAILED));
    }

    @Test
    void runWeeklyPipeline_sourceCollectionFails_continuesWithOtherSources() {
        WeeklyExecution execution = buildExecution("exec-4");
        CommunitySource source1 = buildSource("src-1", "Discord");
        CommunitySource source2 = buildSource("src-2", "Slack");
        AiAnalysis analysis = buildAnalysis("analysis-4");

        when(executionRepository.save(any())).thenReturn(execution);
        when(sourceRepository.findByActiveTrue()).thenReturn(List.of(source1, source2));
        when(activityRepository.findByExecutionId("exec-4")).thenReturn(List.of());
        // First source fails, second succeeds
        doThrow(new RuntimeException("Discord API error")).when(communityCollector).collectActivity("exec-4", "src-1");
        doNothing().when(communityCollector).collectActivity("exec-4", "src-2");
        when(aiAnalyzer.analyzeActivity(anyString(), anyString())).thenReturn(analysis);
        when(draftGenerator.generateDrafts("exec-4")).thenReturn(List.of());

        // Should not throw — continues with other sources
        assertDoesNotThrow(() -> orchestrator.runWeeklyPipeline("ADMIN"));
        verify(communityCollector).collectActivity("exec-4", "src-2");
    }

    @Test
    void retryFailedStep_withFailedExecution_retriesSuccessfully() {
        WeeklyExecution execution = buildExecution("exec-5");
        execution.setStatus(WeeklyExecution.ExecutionStatus.FAILED);
        AiAnalysis analysis = buildAnalysis("analysis-5");

        when(executionRepository.findById("exec-5")).thenReturn(Optional.of(execution));
        when(executionRepository.save(any())).thenReturn(execution);
        when(sourceRepository.findByActiveTrue()).thenReturn(List.of());
        when(activityRepository.findByExecutionId("exec-5")).thenReturn(List.of());
        when(aiAnalyzer.analyzeActivity(anyString(), anyString())).thenReturn(analysis);
        when(draftGenerator.generateDrafts("exec-5")).thenReturn(List.of());

        assertDoesNotThrow(() -> orchestrator.retryFailedStep("exec-5"));
    }

    @Test
    void retryFailedStep_withNonFailedExecution_throwsException() {
        WeeklyExecution execution = buildExecution("exec-6");
        execution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);

        when(executionRepository.findById("exec-6")).thenReturn(Optional.of(execution));

        assertThrows(IllegalStateException.class, () -> orchestrator.retryFailedStep("exec-6"));
    }

    private WeeklyExecution buildExecution(String id) {
        WeeklyExecution e = new WeeklyExecution();
        e.setId(id);
        e.setStatus(WeeklyExecution.ExecutionStatus.RUNNING);
        return e;
    }

    private CommunitySource buildSource(String id, String name) {
        CommunitySource s = new CommunitySource();
        s.setId(id);
        s.setName(name);
        s.setType(CommunitySource.SourceType.DISCORD);
        s.setActive(true);
        return s;
    }

    private AiAnalysis buildAnalysis(String id) {
        AiAnalysis a = new AiAnalysis();
        a.setId(id);
        a.setExecutiveSummary("Test summary");
        return a;
    }

    private Draft buildDraft(String id, Draft.Channel channel) {
        Draft d = new Draft();
        d.setId(id);
        d.setChannel(channel);
        d.setStatus(Draft.DraftStatus.PENDING);
        return d;
    }
}
