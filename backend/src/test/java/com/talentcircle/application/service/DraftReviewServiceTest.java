package com.talentcircle.application.service;

import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.port.in.DraftReviewUseCase;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DraftReviewServiceTest {

    @Mock
    private DraftRepository draftRepository;

    @Mock
    private WeeklyExecutionRepository executionRepository;

    private DraftReviewService draftReviewService;

    @BeforeEach
    void setUp() {
        draftReviewService = new DraftReviewService(draftRepository, executionRepository);
    }

    @Test
    void getDraftDetail_ShouldReturnDraft_WhenDraftExists() {
        // Given
        Draft draft = new Draft();
        draft.setId("123");
        draft.setStatus(Draft.DraftStatus.PENDING);
        draft.setContent("Test content");

        when(draftRepository.findById("123")).thenReturn(Optional.of(draft));

        // When
        DraftReviewUseCase.DraftDetailDto result = draftReviewService.getDraftDetail("123");

        // Then
        assertNotNull(result);
        assertEquals("123", result.id());
        assertEquals("PENDING", result.status());
        assertEquals("Test content", result.content());
    }

    @Test
    void approveDraft_ShouldChangeStatusToApproved_WhenDraftIsPending() {
        // Given
        Draft draft = new Draft();
        draft.setId("123");
        draft.setStatus(Draft.DraftStatus.PENDING);

        when(draftRepository.findById("123")).thenReturn(Optional.of(draft));
        when(draftRepository.save(any(Draft.class))).thenReturn(draft);

        // When
        DraftReviewUseCase.DraftDetailDto result = draftReviewService.approveDraft("123");

        // Then
        assertEquals("APPROVED", result.status());
        verify(draftRepository).save(draft);
    }

    @Test
    void approveDraft_ShouldThrowException_WhenDraftIsNotPending() {
        // Given
        Draft draft = new Draft();
        draft.setId("123");
        draft.setStatus(Draft.DraftStatus.APPROVED);

        when(draftRepository.findById("123")).thenReturn(Optional.of(draft));

        // When & Then
        assertThrows(RuntimeException.class, () -> draftReviewService.approveDraft("123"));
    }

    @Test
    void rejectDraft_ShouldChangeStatusToRejected_WhenDraftExists() {
        // Given
        Draft draft = new Draft();
        draft.setId("123");
        draft.setStatus(Draft.DraftStatus.PENDING);

        when(draftRepository.findById("123")).thenReturn(Optional.of(draft));
        when(draftRepository.save(any(Draft.class))).thenReturn(draft);

        DraftReviewUseCase.RejectRequest request = new DraftReviewUseCase.RejectRequest("Not relevant");

        // When
        DraftReviewUseCase.DraftDetailDto result = draftReviewService.rejectDraft("123", request);

        // Then
        assertEquals("REJECTED", result.status());
        assertEquals("Not relevant", draft.getRejectionReason());
    }
}
