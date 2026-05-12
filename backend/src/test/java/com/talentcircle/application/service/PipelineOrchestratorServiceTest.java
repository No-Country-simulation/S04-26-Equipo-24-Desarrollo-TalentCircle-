package com.talentcircle.application.service;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunitySource;
import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.AiAnalyzerUseCase;
import com.talentcircle.domain.port.in.CommunityCollectorUseCase;
import com.talentcircle.domain.port.in.DraftGeneratorUseCase;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineOrchestratorServiceTest {

    @Mock private WeeklyExecutionRepository executionRepository;
    @Mock private CommunityCollectorUseCase communityCollector;
    @Mock private AiAnalyzerUseCase aiAnalyzer;
    @Mock private DraftGeneratorUseCase draftGenerator;

    private PipelineOrchestratorService orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new PipelineOrchestratorService(
                executionRepository, communityCollector, aiAnalyzer, draftGenerator);
    }

    @Test
    void runWeeklyPipeline_completesSuccessfully() {
        WeeklyExecution execution = buildExecution("exec-1");
        AiAnalysis analysis = buildAnalysis("analysis-1");
        Draft draft = buildDraft("draft-1", Draft.Channel.LINKEDIN);

        when(executionRepository.save(any())).thenReturn(execution);
        when(aiAnalyzer.analyzeActivity(anyString(), anyString())).thenReturn(analysis);
        when(draftGenerator.generateDrafts("exec-1")).thenReturn(List.of(draft));
        doNothing().when(communityCollector).collectFromAllActiveSources(anyString());

        String executionId = orchestrator.runWeeklyPipeline("ADMIN");

        assertNotNull(executionId);
        verify(communityCollector).collectFromAllActiveSources("exec-1");
        verify(aiAnalyzer).analyzeActivity(eq("exec-1"), anyString());
        verify(draftGenerator).generateDrafts("exec-1");
    }

    @Test
    void runWeeklyPipeline_whenAiFails_marksExecutionAsFailed() {
        WeeklyExecution execution = buildExecution("exec-3");

        when(executionRepository.save(any())).thenReturn(execution);
        doNothing().when(communityCollector).collectFromAllActiveSources(anyString());
        when(aiAnalyzer.analyzeActivity(anyString(), anyString()))
                .thenThrow(new RuntimeException("LLM API unavailable"));

        assertThrows(RuntimeException.class, () -> orchestrator.runWeeklyPipeline("ADMIN"));
        assertEquals(WeeklyExecution.ExecutionStatus.FAILED, execution.getStatus());
    }

    @Test
    void retryFailedStep_withFailedExecution_retriesSuccessfully() {
        WeeklyExecution execution = buildExecution("exec-5");
        execution.setStatus(WeeklyExecution.ExecutionStatus.FAILED);
        AiAnalysis analysis = buildAnalysis("analysis-5");

        when(executionRepository.findById("exec-5")).thenReturn(Optional.of(execution));
        when(executionRepository.save(any())).thenReturn(execution);
        doNothing().when(communityCollector).collectFromAllActiveSources(anyString());
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
