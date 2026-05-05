package com.talentcircle.application.service;

import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.port.in.DraftReviewUseCase;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DraftReviewService implements DraftReviewUseCase {

    private final DraftRepository draftRepository;
    private final WeeklyExecutionRepository executionRepository;

    public DraftReviewService(DraftRepository draftRepository,
                                WeeklyExecutionRepository executionRepository) {
        this.draftRepository = draftRepository;
        this.executionRepository = executionRepository;
    }

    @Override
    public List<DraftSummaryDto> listDrafts(String channel, String status, String weekStart, String weekEnd, int page, int size) {
        // Implementation: fetch from DB with filters
        List<Draft> drafts;
        if (status != null) {
            drafts = draftRepository.findByStatus(Draft.DraftStatus.valueOf(status));
        } else {
            drafts = draftRepository.findAll();
        }
        return drafts.stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    public DraftDetailDto getDraftDetail(String draftId) {
        Draft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found"));
        return mapToDetailDto(draft);
    }

    @Override
    public DraftDetailDto updateContent(String draftId, UpdateContentRequest request) {
        Draft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found"));

        // Save current content as version before updating
        // Implementation: saveToVersion(draft);

        draft.setEditedContent(request.content());
        Draft saved = draftRepository.save(draft);
        return mapToDetailDto(saved);
    }

    @Override
    public DraftDetailDto approveDraft(String draftId) {
        Draft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found"));

        if (draft.getStatus() != Draft.DraftStatus.PENDING) {
            throw new RuntimeException("Only PENDING drafts can be approved");
        }

        draft.setStatus(Draft.DraftStatus.APPROVED);
        Draft saved = draftRepository.save(draft);
        return mapToDetailDto(saved);
    }

    @Override
    public DraftDetailDto rejectDraft(String draftId, RejectRequest request) {
        Draft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found"));

        draft.setStatus(Draft.DraftStatus.REJECTED);
        draft.setRejectionReason(request.reason());
        Draft saved = draftRepository.save(draft);
        return mapToDetailDto(saved);
    }

    private DraftSummaryDto mapToSummaryDto(Draft draft) {
        return new DraftSummaryDto(
                draft.getId(),
                draft.getChannel() != null ? draft.getChannel().name() : null,
                draft.getStatus() != null ? draft.getStatus().name() : null,
                draft.getCreatedAt() != null ? draft.getCreatedAt().toString() : null,
                draft.getContent() != null ?
                        (draft.getContent().length() > 100 ?
                                draft.getContent().substring(0, 100) + "..." : draft.getContent())
                        : null
        );
    }

    private DraftDetailDto mapToDetailDto(Draft draft) {
        // Map sources and versions
        return new DraftDetailDto(
                draft.getId(),
                draft.getChannel() != null ? draft.getChannel().name() : null,
                draft.getContent(),
                draft.getEditedContent(),
                draft.getStatus() != null ? draft.getStatus().name() : null,
                draft.getAiScore(),
                draft.getCreatedAt() != null ? draft.getCreatedAt().toString() : null,
                draft.getUpdatedAt() != null ? draft.getUpdatedAt().toString() : null,
                List.of(), // sources
                List.of()  // versions
        );
    }
}
