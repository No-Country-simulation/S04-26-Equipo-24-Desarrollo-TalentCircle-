package com.talentcircle.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "draft_sources")
public class DraftSource extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id")
    private Draft draft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private CommunityActivity activity;

    @Column(name = "relevance_score")
    private Double relevanceScore;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    // Getters and Setters
    public Draft getDraft() { return draft; }
    public void setDraft(Draft draft) { this.draft = draft; }

    public CommunityActivity getActivity() { return activity; }
    public void setActivity(CommunityActivity activity) { this.activity = activity; }

    public Double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
