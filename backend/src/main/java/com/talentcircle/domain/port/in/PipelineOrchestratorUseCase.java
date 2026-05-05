package com.talentcircle.domain.port.in;

public interface PipelineOrchestratorUseCase {
    String runWeeklyPipeline(String triggeredBy);
    void retryFailedStep(String executionId);
}
