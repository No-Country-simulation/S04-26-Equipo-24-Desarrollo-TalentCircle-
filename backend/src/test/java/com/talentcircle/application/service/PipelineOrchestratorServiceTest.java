package com.talentcircle.application.service;

import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.AiAnalyzerUseCase;
import com.talentcircle.domain.port.in.CommunityCollectorUseCase;
import com.talentcircle.domain.port.in.DraftGeneratorUseCase;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineOrchestratorServiceTest {

    @Mock
    private WeeklyExecutionRepository executionRepository;

    @Mock
    private CommunityCollectorUseCase communityCollectorUseCase;

    @Mock
    private AiAnalyzerUseCase aiAnalyzerUseCase;

    @Mock
    private DraftGeneratorUseCase draftGeneratorUseCase;

    @InjectMocks
    private PipelineOrchestratorService orchestratorService;

    @BeforeEach
    void setUp() {
        // Use lenient stubbing to avoid UnnecessaryStubbingException
        Mockito.lenient().when(executionRepository.save(any(WeeklyExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void runWeeklyPipeline_shouldExecuteAllStepsSuccessfully() {
        String executionId = orchestratorService.runWeeklyPipeline("test-user");

        assertNotNull(executionId);

        verify(communityCollectorUseCase, times(1)).collectActivity(anyString(), anyString());
        verify(aiAnalyzerUseCase, times(1)).analyzeActivity(anyString(), anyString());
        verify(draftGeneratorUseCase, times(1)).generateDrafts(anyString());
        verify(executionRepository, atLeastOnce()).save(any(WeeklyExecution.class));
    }

    @Test
    void runWeeklyPipeline_shouldHandleFailure() {
        doThrow(new RuntimeException("Collection failed"))
                .when(communityCollectorUseCase).collectActivity(anyString(), anyString());

        assertThrows(RuntimeException.class, () ->
                orchestratorService.runWeeklyPipeline("test-user")
        );

        // Verify save was called (at least once for the initial save and once after failure)
        verify(executionRepository, atLeast(1)).save(any(WeeklyExecution.class));
    }

    @Test
    void retryFailedStep_shouldRetryPipeline() {
        WeeklyExecution testExecution = new WeeklyExecution();
        testExecution.setId("exec-123");
        testExecution.setStatus(WeeklyExecution.ExecutionStatus.FAILED);

        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));

        orchestratorService.retryFailedStep("exec-123");

        assertEquals(WeeklyExecution.ExecutionStatus.COMPLETED, testExecution.getStatus());
        assertNotNull(testExecution.getCompletedAt());

        verify(communityCollectorUseCase, times(1)).collectActivity(anyString(), anyString());
        verify(aiAnalyzerUseCase, times(1)).analyzeActivity(anyString(), anyString());
        verify(draftGeneratorUseCase, times(1)).generateDrafts(anyString());
    }

    @Test
    void retryFailedStep_shouldThrowExceptionWhenExecutionNotFound() {
        when(executionRepository.findById("invalid-exec")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orchestratorService.retryFailedStep("invalid-exec")
        );

        assertEquals("Execution not found: invalid-exec", exception.getMessage());
        verify(communityCollectorUseCase, never()).collectActivity(anyString(), anyString());
    }

    @Test
    void retryFailedStep_shouldThrowExceptionWhenNotFailed() {
        WeeklyExecution testExecution = new WeeklyExecution();
        testExecution.setId("exec-123");
        testExecution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);

        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                orchestratorService.retryFailedStep("exec-123")
        );

        assertTrue(exception.getMessage().contains("Only FAILED executions can be retried"));
        verify(communityCollectorUseCase, never()).collectActivity(anyString(), anyString());
    }
}
