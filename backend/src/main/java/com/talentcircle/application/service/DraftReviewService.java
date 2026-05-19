package com.talentcircle.application.service;

import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.DraftSource;
import com.talentcircle.domain.model.DraftVersion;
import com.talentcircle.domain.port.in.DraftReviewUseCase;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.DraftSourceRepository;
import com.talentcircle.domain.port.out.DraftVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the editorial review lifecycle for drafts.
 * RF-17 to RF-22: Panel de Revisión Editorial.
 */
@Service
@Transactional
public class DraftReviewService implements DraftReviewUseCase {

    private static final Logger log = LoggerFactory.getLogger(DraftReviewService.class);

    private final DraftRepository draftRepository;
    private final DraftVersionRepository versionRepository;
    private final DraftSourceRepository sourceRepository;

    public DraftReviewService(DraftRepository draftRepository,
                              DraftVersionRepository versionRepository,
                              DraftSourceRepository sourceRepository) {
        this.draftRepository = draftRepository;
        this.versionRepository = versionRepository;
        this.sourceRepository = sourceRepository;
    }

    // RF-17: Listado paginado con filtros
    @Override
    public List<DraftSummaryDto> listDrafts(String channel, String status, String weekStart, String weekEnd,
                                             int page, int size) {
        List<Draft> drafts;
        if (status != null && !status.isBlank()) {
            drafts = draftRepository.findByStatus(Draft.DraftStatus.valueOf(status.toUpperCase()));
        } else {
            drafts = draftRepository.findAll();
        }

        // Apply channel filter
        if (channel != null && !channel.isBlank()) {
            Draft.Channel ch = Draft.Channel.valueOf(channel.toUpperCase());
            drafts = drafts.stream().filter(d -> ch.equals(d.getChannel())).collect(Collectors.toList());
        }

        return drafts.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .skip((long) page * size)
                .limit(size)
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    // RF-18: Vista detalle con fuentes y versiones
    @Override
    public DraftDetailDto getDraftDetail(String draftId) {
        Draft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found: " + draftId));
        return mapToDetailDto(draft);
    }

    // RF-19: Edición inline — guarda versión anterior antes de actualizar
    @Override
    public DraftDetailDto updateContent(String draftId, UpdateContentRequest request) {
        Draft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found: " + draftId));

        // RF-16: Save current content as a new version before updating
        saveVersion(draft);

        draft.setEditedContent(request.content());
        Draft saved = draftRepository.save(draft);
        log.info("Draft content updated. draftId={}", draftId);
        return mapToDetailDto(saved);
    }

    // RF-20: Aprobación
    @Override
    public DraftDetailDto approveDraft(String draftId) {
        Draft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found: " + draftId));

        if (draft.getStatus() == Draft.DraftStatus.APPROVED) {
            throw new IllegalStateException("Draft is already approved: " + draftId);
        }
        if (draft.getStatus() != Draft.DraftStatus.PENDING) {
            throw new IllegalStateException("Only PENDING drafts can be approved. Current status: " + draft.getStatus());
        }

        if (draft.getChannel() == Draft.Channel.NEWSLETTER) {
            draft.setStatus(Draft.DraftStatus.PUBLISHED);
            log.info("Newsletter draft auto-published to landing page. draftId={}", draftId);
        } else {
            draft.setStatus(Draft.DraftStatus.APPROVED);
        }
        draft.setApprovedAt(LocalDateTime.now());
        draft.setApprovedBy(null); // In full impl: resolve User from SecurityContext

        Draft saved = draftRepository.save(draft);
        log.info("Draft approved. draftId={}", draftId);
        return mapToDetailDto(saved);
    }

    // RF-20: Rechazo con comentario obligatorio
    @Override
    public DraftDetailDto rejectDraft(String draftId, RejectRequest request) {
        Draft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found: " + draftId));

        if (request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        draft.setStatus(Draft.DraftStatus.REJECTED);
        draft.setRejectionReason(request.reason());
        Draft saved = draftRepository.save(draft);
        log.info("Draft rejected. draftId={}, reason={}", draftId, request.reason());
        return mapToDetailDto(saved);
    }

    // RF-16: Save current content as a versioned snapshot
    private void saveVersion(Draft draft) {
        String contentToVersion = draft.getEditedContent() != null ? draft.getEditedContent() : draft.getContent();
        if (contentToVersion == null) return;

        int nextVersion = versionRepository.countByDraftId(draft.getId()) + 1;
        String editorId = getCurrentUserId();

        DraftVersion version = new DraftVersion();
        version.setDraft(draft);
        version.setContent(contentToVersion);
        version.setEditedBy(editorId);
        version.setVersionNumber(nextVersion);
        versionRepository.save(version);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private DraftSummaryDto mapToSummaryDto(Draft draft) {
        String preview = draft.getContent() != null
                ? (draft.getContent().length() > 100 ? draft.getContent().substring(0, 100) + "..." : draft.getContent())
                : null;
        return new DraftSummaryDto(
                draft.getId(),
                draft.getChannel() != null ? draft.getChannel().name() : null,
                draft.getStatus() != null ? draft.getStatus().name() : null,
                draft.getCreatedAt() != null ? draft.getCreatedAt().toString() : null,
                preview
        );
    }

    private DraftDetailDto mapToDetailDto(Draft draft) {
        List<SourceDto> sources = sourceRepository.findByDraftId(draft.getId()).stream()
                .map(s -> new SourceDto(
                        s.getId(),
                        s.getActivity() != null ? s.getActivity().getTitle() : null,
                        s.getRelevanceScore()))
                .toList();

        List<DraftVersionDto> versions = versionRepository.findByDraftIdOrderByVersionNumberAsc(draft.getId()).stream()
                .map(v -> new DraftVersionDto(
                        v.getId(),
                        v.getContent(),
                        v.getEditedBy(),
                        v.getCreatedAt() != null ? v.getCreatedAt().toString() : null,
                        v.getVersionNumber()))
                .toList();

        return new DraftDetailDto(
                draft.getId(),
                draft.getChannel() != null ? draft.getChannel().name() : null,
                draft.getContent(),
                draft.getEditedContent(),
                draft.getStatus() != null ? draft.getStatus().name() : null,
                draft.getAiScore(),
                draft.getCreatedAt() != null ? draft.getCreatedAt().toString() : null,
                draft.getUpdatedAt() != null ? draft.getUpdatedAt().toString() : null,
                sources,
                versions
        );
    }
}
