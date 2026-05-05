package com.talentcircle.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "drafts")
public class Draft extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id")
    private WeeklyExecution execution;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel; // NEWSLETTER, LINKEDIN, TWITTER

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "edited_content", columnDefinition = "TEXT")
    private String editedContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftStatus status; // PENDING, APPROVED, REJECTED, PUBLISHED

    @Column(name = "ai_score")
    private Double aiScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    public enum Channel {
        NEWSLETTER, LINKEDIN, TWITTER
    }

    public enum DraftStatus {
        PENDING, APPROVED, REJECTED, PUBLISHED
    }

    // Getters and Setters
    public WeeklyExecution getExecution() { return execution; }
    public void setExecution(WeeklyExecution execution) { this.execution = execution; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getEditedContent() { return editedContent; }
    public void setEditedContent(String editedContent) { this.editedContent = editedContent; }

    public DraftStatus getStatus() { return status; }
    public void setStatus(DraftStatus status) { this.status = status; }

    public Double getAiScore() { return aiScore; }
    public void setAiScore(Double aiScore) { this.aiScore = aiScore; }

    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
