package com.talentcircle.domain.event;

import org.springframework.context.ApplicationEvent;

public class AnalysisCompletedEvent extends ApplicationEvent {

    private final String executionId;
    private final String analysisId;

    public AnalysisCompletedEvent(Object source, String executionId, String analysisId) {
        super(source);
        this.executionId = executionId;
        this.analysisId = analysisId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getAnalysisId() {
        return analysisId;
    }
}
