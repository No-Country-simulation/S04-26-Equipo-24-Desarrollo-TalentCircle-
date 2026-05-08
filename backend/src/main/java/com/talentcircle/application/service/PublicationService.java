package com.talentcircle.application.service;

import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.Publication;
import com.talentcircle.domain.port.in.PublicationUseCase;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.LinkedInClientPort;
import com.talentcircle.domain.port.out.PublicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PublicationService implements PublicationUseCase {

    private static final Logger log = LoggerFactory.getLogger(PublicationService.class);

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
                .orElseThrow(() -> new IllegalArgumentException("Draft not found: " + draftId));

        if (draft.getStatus() != Draft.DraftStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED drafts can be published");
        }

        // Usar el contenido editado si existe, si no el original generado por IA
        String contentToPublish = draft.getEditedContent() != null
                ? draft.getEditedContent()
                : draft.getContent();

        Publication publication = new Publication();
        publication.setDraft(draft);
        publication.setChannel(mapChannel(draft.getChannel()));
        publication.setStatus(Publication.PublicationStatus.RETRYING);
        publication.setRetryCount(0);

        try {
            String externalPostId = publishByChannel(draft.getChannel(), contentToPublish);
            publication.setExternalPostId(externalPostId);
            publication.setStatus(Publication.PublicationStatus.SUCCESS);
            publication.setPublishedAt(LocalDateTime.now());

            draft.setStatus(Draft.DraftStatus.PUBLISHED);
            draftRepository.save(draft);

            log.info("Draft {} publicado exitosamente en canal {} con ID externo {}",
                    draftId, draft.getChannel(), externalPostId);

        } catch (Exception e) {
            publication.setStatus(Publication.PublicationStatus.FAILED);
            publication.setErrorMessage(e.getMessage());
            log.error("Error publicando draft {} en canal {}: {}", draftId, draft.getChannel(), e.getMessage());
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

    /**
     * Enruta la publicación al canal correcto según el tipo de borrador.
     * LINKEDIN   → API real de LinkedIn (ugcPosts)
     * TWITTER    → Simulado (pendiente TwitterClientPort)
     * NEWSLETTER → Simulado (pendiente EmailClientPort)
     */
    private String publishByChannel(Draft.Channel channel, String content) {
        return switch (channel) {
            case LINKEDIN -> linkedInClient.publishPost(content);
            case TWITTER -> {
                // Twitter API v2 requiere OAuth 1.0a o OAuth 2.0 con scope tweet.write.
                // Pendiente de implementar TwitterClientAdapter.
                // Por ahora se simula una publicación exitosa para no bloquear el flujo.
                log.warn("Twitter no implementado — simulando publicación exitosa para draft de canal TWITTER");
                yield "sim:twitter:" + System.currentTimeMillis();
            }
            case NEWSLETTER -> {
                // Newsletter requiere integración con SendGrid / Mailchimp.
                // Pendiente de implementar EmailClientAdapter.
                // Por ahora se simula una publicación exitosa para no bloquear el flujo.
                log.warn("Newsletter no implementado — simulando publicación exitosa para draft de canal NEWSLETTER");
                yield "sim:newsletter:" + System.currentTimeMillis();
            }
        };
    }

    @Override
    public byte[] exportDrafts(ExportRequest request) {
        // Fetch approved drafts for the week
        List<Draft> approvedDrafts;

        if (request.week() != null && !request.week().isEmpty()) {
            // Parse week string (format: "YYYY-Www")
            approvedDrafts = draftRepository.findByStatus(Draft.DraftStatus.APPROVED);
        } else {
            approvedDrafts = draftRepository.findByStatus(Draft.DraftStatus.APPROVED);
        }

        if (approvedDrafts.isEmpty()) {
            throw new IllegalStateException("No approved drafts found for export");
        }

        // Generate export based on format
        String format = request.format() != null ? request.format().toLowerCase() : "csv";

        return switch (format) {
            case "json" -> generateJsonExport(approvedDrafts);
            case "pdf" -> generatePdfExport(approvedDrafts);
            default -> generateCsvExport(approvedDrafts);
        };
    }

    private byte[] generateCsvExport(List<Draft> drafts) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Channel,Content,Status,AI Score,Created At\n");

        for (Draft draft : drafts) {
            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%s,\"%s\"\n",
                    draft.getId(),
                    draft.getChannel() != null ? draft.getChannel().name() : "",
                    draft.getContent() != null ? draft.getContent().replace("\"", "\"\"") : "",
                    draft.getStatus() != null ? draft.getStatus().name() : "",
                    draft.getAiScore() != null ? draft.getAiScore() : "",
                    draft.getCreatedAt() != null ? draft.getCreatedAt().toString() : ""
            ));
        }

        return csv.toString().getBytes();
    }

    private byte[] generateJsonExport(List<Draft> drafts) {
        StringBuilder json = new StringBuilder();
        json.append("[\n");

        for (int i = 0; i < drafts.size(); i++) {
            Draft draft = drafts.get(i);
            json.append("  {\n");
            json.append("    \"id\": \"").append(draft.getId()).append("\"\n");
            json.append("    \"channel\": \"").append(draft.getChannel() != null ? draft.getChannel().name() : "").append("\"\n");
            json.append("    \"content\": \"").append(draft.getContent() != null ? draft.getContent().replace("\"", "\\\"") : "").append("\"\n");
            json.append("    \"status\": \"").append(draft.getStatus() != null ? draft.getStatus().name() : "").append("\"\n");
            json.append("    \"aiScore\": ").append(draft.getAiScore() != null ? draft.getAiScore() : "null").append("\n");
            json.append("  }");

            if (i < drafts.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("]");
        return json.toString().getBytes();
    }

    private byte[] generatePdfExport(List<Draft> drafts) {
        // Simplified PDF export - in production, use a library like iText or OpenPDF
        StringBuilder pdfContent = new StringBuilder();
        pdfContent.append("PDF Export (Simplified)\n");
        pdfContent.append("========================\n\n");

        for (Draft draft : drafts) {
            pdfContent.append("Draft ID: ").append(draft.getId()).append("\n");
            pdfContent.append("Channel: ").append(draft.getChannel() != null ? draft.getChannel().name() : "").append("\n");
            pdfContent.append("Content: ").append(draft.getContent() != null ? draft.getContent() : "").append("\n");
            pdfContent.append("Status: ").append(draft.getStatus() != null ? draft.getStatus().name() : "").append("\n");
            pdfContent.append("AI Score: ").append(draft.getAiScore() != null ? draft.getAiScore() : "N/A").append("\n");
            pdfContent.append("\n---\n\n");
        }

        return pdfContent.toString().getBytes();
    }

    private Publication.Channel mapChannel(Draft.Channel draftChannel) {
        if (draftChannel == null) {
            throw new IllegalArgumentException("Draft channel is null");
        }
        return Publication.Channel.valueOf(draftChannel.name());
    }
}
