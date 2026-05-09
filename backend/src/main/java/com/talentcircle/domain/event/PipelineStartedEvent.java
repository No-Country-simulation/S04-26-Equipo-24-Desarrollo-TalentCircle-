package com.talentcircle.domain.event;

import org.springframework.context.ApplicationEvent;

public class PipelineStartedEvent extends ApplicationEvent {

    private final String executionId;
    private final String triggeredBy;

    public PipelineStartedEvent(Object source, String executionId, String triggeredBy) {
        super(source);
        this.executionId = executionId;
        this.triggeredBy = triggeredBy;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }
}
