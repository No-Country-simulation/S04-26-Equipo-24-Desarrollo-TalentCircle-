package com.talentcircle.domain.event;

import org.springframework.context.ApplicationEvent;

public class DraftPublishedEvent extends ApplicationEvent {

    private final String draftId;
    private final String externalPostId;
    private final String channel;

    public DraftPublishedEvent(Object source, String draftId, String externalPostId, String channel) {
        super(source);
        this.draftId = draftId;
        this.externalPostId = externalPostId;
        this.channel = channel;
    }

    public String getDraftId() {
        return draftId;
    }

    public String getExternalPostId() {
        return externalPostId;
    }

    public String getChannel() {
        return channel;
    }
}
