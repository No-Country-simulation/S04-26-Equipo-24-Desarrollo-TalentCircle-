package com.talentcircle.application.service;

import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.Publication;
import com.talentcircle.domain.port.in.PublicationUseCase;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.LinkedInClientPort;
import com.talentcircle.domain.port.out.PublicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublicationService implements PublicationUseCase {

    private final DraftRepository draftRepository;
    private final PublicationRepository publicationRepository;
    private final LinkedInClientPort linkedInClient;

    public PublicationService(DraftRepository draftRepository,
                                PublicationRepository publicationRepository,
                                LinkedInClientPort linkedInClient) {
        this.draftRepository = draftRepository;
        this.publicationRepository = publicationRepository;
        this.linkedInClient = linkedInClient;
    }

    @Override
    public PublicationDto publishDraft(String draftId) {
        Draft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found"));

        if (draft.getStatus() != Draft.DraftStatus.APPROVED) {
            throw new RuntimeException("Only APPROVED drafts can be published");
        }

        Publication publication = new Publication();
        publication.setDraft(draft);
        publication.setChannel(mapChannel(draft.getChannel()));
        publication.setStatus(Publication.PublicationStatus.RETRYING);

        // Call LinkedIn API
        try {
            String externalPostId = linkedInClient.publishPost(draft.getContent());
            publication.setExternalPostId(externalPostId);
            publication.setStatus(Publication.PublicationStatus.SUCCESS);
            publication.setPublishedAt(java.time.LocalDateTime.now());
            
            draft.setStatus(Draft.DraftStatus.PUBLISHED);
            draftRepository.save(draft);
        } catch (Exception e) {
            publication.setStatus(Publication.PublicationStatus.FAILED);
            publication.setErrorMessage(e.getMessage());
        }

        publication = publicationRepository.save(publication);
        
        return new PublicationDto(
            publication.getId(),
            publication.getDraft().getId(),
            publication.getStatus().name(),
            publication.getExternalPostId(),
            publication.getPublishedAt() != null ? publication.getPublishedAt().toString() : null,
            publication.getErrorMessage()
        );
    }

    @Override
    public byte[] exportDrafts(ExportRequest request) {
        // Implementation: fetch approved drafts and generate export
        throw new RuntimeException("Export not implemented yet");
    }

    private Publication.Channel mapChannel(Draft.Channel draftChannel) {
        return Publication.Channel.valueOf(draftChannel.name());
    }
}
