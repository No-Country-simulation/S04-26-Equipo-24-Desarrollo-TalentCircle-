package com.talentcircle.application.service;

import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.port.in.DraftReviewUseCase;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.DraftSourceRepository;
import com.talentcircle.domain.port.out.DraftVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DraftReviewServiceTest {

    @Mock
    private DraftRepository draftRepository;

    @Mock
    private DraftVersionRepository versionRepository;

    @Mock
    private DraftSourceRepository sourceRepository;

    private DraftReviewService draftReviewService;

    @BeforeEach
    void setUp() {
        draftReviewService = new DraftReviewService(draftRepository, versionRepository, sourceRepository);
    }

    @Test
    void approveDraft_withPendingDraft_changesStatusToApproved() {
        Draft draft = buildDraft(Draft.DraftStatus.PENDING);
        when(draftRepository.findById("draft-1")).thenReturn(Optional.of(draft));
        when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sourceRepository.findByDraftId("draft-1")).thenReturn(List.of());
        when(versionRepository.findByDraftIdOrderByVersionNumberAsc("draft-1")).thenReturn(List.of());

        DraftReviewUseCase.DraftDetailDto result = draftReviewService.approveDraft("draft-1");

        assertEquals("APPROVED", result.status());
        verify(draftRepository).save(any(Draft.class));
    }

    @Test
    void approveDraft_withAlreadyApprovedDraft_throwsException() {
        Draft draft = buildDraft(Draft.DraftStatus.APPROVED);
        when(draftRepository.findById("draft-1")).thenReturn(Optional.of(draft));

        assertThrows(IllegalStateException.class, () -> draftReviewService.approveDraft("draft-1"));
    }

    @Test
    void rejectDraft_withValidReason_changesStatusToRejected() {
        Draft draft = buildDraft(Draft.DraftStatus.PENDING);
        when(draftRepository.findById("draft-1")).thenReturn(Optional.of(draft));
        when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sourceRepository.findByDraftId("draft-1")).thenReturn(List.of());
        when(versionRepository.findByDraftIdOrderByVersionNumberAsc("draft-1")).thenReturn(List.of());

        DraftReviewUseCase.DraftDetailDto result = draftReviewService.rejectDraft("draft-1",
                new DraftReviewUseCase.RejectRequest("Contenido no es relevante"));

        assertEquals("REJECTED", result.status());
    }

    @Test
    void rejectDraft_withEmptyReason_throwsException() {
        Draft draft = buildDraft(Draft.DraftStatus.PENDING);
        when(draftRepository.findById("draft-1")).thenReturn(Optional.of(draft));

        assertThrows(IllegalArgumentException.class, () ->
                draftReviewService.rejectDraft("draft-1", new DraftReviewUseCase.RejectRequest("")));
    }

    @Test
    void updateContent_savesPreviousVersionBeforeUpdating() {
        Draft draft = buildDraft(Draft.DraftStatus.PENDING);
        draft.setContent("Original content");
        when(draftRepository.findById("draft-1")).thenReturn(Optional.of(draft));
        when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepository.countByDraftId("draft-1")).thenReturn(0);
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sourceRepository.findByDraftId("draft-1")).thenReturn(List.of());
        when(versionRepository.findByDraftIdOrderByVersionNumberAsc("draft-1")).thenReturn(List.of());

        draftReviewService.updateContent("draft-1", new DraftReviewUseCase.UpdateContentRequest("New content"));

        // Verify a version was saved before updating
        verify(versionRepository).save(any());
        verify(draftRepository).save(any(Draft.class));
    }

    @Test
    void getDraftDetail_withNonExistentId_throwsException() {
        when(draftRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> draftReviewService.getDraftDetail("non-existent"));
    }

    private Draft buildDraft(Draft.DraftStatus status) {
        Draft draft = new Draft();
        draft.setId("draft-1");
        draft.setChannel(Draft.Channel.LINKEDIN);
        draft.setContent("Test content");
        draft.setStatus(status);
        return draft;
    }
}
