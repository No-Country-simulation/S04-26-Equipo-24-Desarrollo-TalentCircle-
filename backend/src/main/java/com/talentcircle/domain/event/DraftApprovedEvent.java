package com.talentcircle.domain.event;

import org.springframework.context.ApplicationEvent;

public class DraftApprovedEvent extends ApplicationEvent {

    private final String draftId;
    private final String approvedBy;

    public DraftApprovedEvent(Object source, String draftId, String approvedBy) {
        super(source);
        this.draftId = draftId;
        this.approvedBy = approvedBy;
    }

    public String getDraftId() {
        return draftId;
    }

    public String getApprovedBy() {
        return approvedBy;
    }
}
