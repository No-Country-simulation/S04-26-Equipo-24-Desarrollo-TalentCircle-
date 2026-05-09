package com.talentcircle.domain.event;

import org.springframework.context.ApplicationEvent;
import java.util.List;

public class DraftsGeneratedEvent extends ApplicationEvent {

    private final String executionId;
    private final List<String> draftIds;

    public DraftsGeneratedEvent(Object source, String executionId, List<String> draftIds) {
        super(source);
        this.executionId = executionId;
        this.draftIds = draftIds;
    }

    public String getExecutionId() {
        return executionId;
    }

    public List<String> getDraftIds() {
        return draftIds;
    }
}
