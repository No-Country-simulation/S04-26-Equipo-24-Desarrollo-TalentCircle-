package com.talentcircle.domain.event;

import org.springframework.context.ApplicationEvent;

public class ActivityCollectedEvent extends ApplicationEvent {

    private final String executionId;
    private final int itemCount;

    public ActivityCollectedEvent(Object source, String executionId, int itemCount) {
        super(source);
        this.executionId = executionId;
        this.itemCount = itemCount;
    }

    public String getExecutionId() {
        return executionId;
    }

    public int getItemCount() {
        return itemCount;
    }
}
