package com.talentcircle.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DraftTest {

    @Test
    void createDraft_ShouldSetPropertiesCorrectly() {
        // Given
        Draft draft = new Draft();
        WeeklyExecution execution = new WeeklyExecution();
        execution.setId("exec-123");

        // When
        draft.setExecution(execution);
        draft.setChannel(Draft.Channel.LINKEDIN);
        draft.setContent("Test content");
        draft.setStatus(Draft.DraftStatus.PENDING);
        draft.setAiScore(0.85);

        // Then
        assertEquals("exec-123", draft.getExecution().getId());
        assertEquals(Draft.Channel.LINKEDIN, draft.getChannel());
        assertEquals("Test content", draft.getContent());
        assertEquals(Draft.DraftStatus.PENDING, draft.getStatus());
        assertEquals(0.85, draft.getAiScore());
    }

    @Test
    void draftStatus_ShouldHaveCorrectValues() {
        assertEquals(4, Draft.DraftStatus.values().length);
        assertNotNull(Draft.DraftStatus.PENDING);
        assertNotNull(Draft.DraftStatus.APPROVED);
        assertNotNull(Draft.DraftStatus.REJECTED);
        assertNotNull(Draft.DraftStatus.PUBLISHED);
    }

    @Test
    void channel_ShouldHaveCorrectValues() {
        assertEquals(3, Draft.Channel.values().length);
        assertNotNull(Draft.Channel.NEWSLETTER);
        assertNotNull(Draft.Channel.LINKEDIN);
        assertNotNull(Draft.Channel.TWITTER);
    }

    @Test
    void updateContent_ShouldSetEditedContent() {
        // Given
        Draft draft = new Draft();
        draft.setContent("Original content");

        // When
        draft.setEditedContent("Edited content");

        // Then
        assertEquals("Original content", draft.getContent());
        assertEquals("Edited content", draft.getEditedContent());
    }
}
