package com.talentcircle.application.service;

import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.Publication;
import com.talentcircle.domain.port.in.PublicationUseCase;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.LinkedInClientPort;
import com.talentcircle.domain.port.out.PublicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicationServiceTest {

    @Mock
    private DraftRepository draftRepository;

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private LinkedInClientPort linkedInClient;

    @InjectMocks
    private PublicationService publicationService;

    private Draft testDraft;

    @BeforeEach
    void setUp() {
        testDraft = new Draft();
        testDraft.setId("draft-123");
        testDraft.setStatus(Draft.DraftStatus.APPROVED);
        testDraft.setContent("Test draft content for publication");
        testDraft.setChannel(Draft.Channel.LINKEDIN);
    }

    @Test
    void publishDraft_shouldPublishSuccessfully() {
        when(draftRepository.findById("draft-123")).thenReturn(Optional.of(testDraft));
        when(linkedInClient.publishPost("Test draft content for publication")).thenReturn("ext-post-456");
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));
        when(draftRepository.save(any(Draft.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicationUseCase.PublicationDto result = publicationService.publishDraft("draft-123");

        assertNotNull(result);
        assertEquals("draft-123", result.draftId());
        assertEquals(Publication.PublicationStatus.SUCCESS.name(), result.status());
        assertEquals("ext-post-456", result.externalPostId());
        assertNotNull(result.publishedAt());

        assertEquals(Draft.DraftStatus.PUBLISHED, testDraft.getStatus());
        verify(publicationRepository, times(1)).save(any(Publication.class));
    }

    @Test
    void publishDraft_shouldHandleLinkedInFailure() {
        when(draftRepository.findById("draft-123")).thenReturn(Optional.of(testDraft));
        when(linkedInClient.publishPost(anyString())).thenThrow(new RuntimeException("LinkedIn API error"));
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicationUseCase.PublicationDto result = publicationService.publishDraft("draft-123");

        assertNotNull(result);
        assertEquals(Publication.PublicationStatus.FAILED.name(), result.status());
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("LinkedIn API error"));

        // Draft status should remain APPROVED (not changed to PUBLISHED)
        assertEquals(Draft.DraftStatus.APPROVED, testDraft.getStatus());
    }

    @Test
    void publishDraft_shouldThrowExceptionWhenDraftNotFound() {
        when(draftRepository.findById("invalid-draft")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                publicationService.publishDraft("invalid-draft")
        );

        assertEquals("Draft not found: invalid-draft", exception.getMessage());
        verify(linkedInClient, never()).publishPost(anyString());
        verify(publicationRepository, never()).save(any(Publication.class));
    }

    @Test
    void publishDraft_shouldThrowExceptionWhenDraftNotApproved() {
        testDraft.setStatus(Draft.DraftStatus.PENDING);
        when(draftRepository.findById("draft-123")).thenReturn(Optional.of(testDraft));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                publicationService.publishDraft("draft-123")
        );

        assertTrue(exception.getMessage().contains("Only APPROVED drafts can be published"));
        verify(linkedInClient, never()).publishPost(anyString());
    }

    @Test
    void exportDrafts_shouldGenerateCsv() {
        testDraft.setStatus(Draft.DraftStatus.APPROVED);
        when(draftRepository.findByStatus(Draft.DraftStatus.APPROVED)).thenReturn(List.of(testDraft));

        PublicationUseCase.ExportRequest request = new PublicationUseCase.ExportRequest("2026-W18", "csv");
        byte[] result = publicationService.exportDrafts(request);

        assertNotNull(result);
        String csv = new String(result);
        assertTrue(csv.contains("ID,Channel,Content,Status,AI Score,Created At"));
        assertTrue(csv.contains("draft-123"));
    }

    @Test
    void exportDrafts_shouldGenerateJson() {
        testDraft.setStatus(Draft.DraftStatus.APPROVED);
        when(draftRepository.findByStatus(Draft.DraftStatus.APPROVED)).thenReturn(List.of(testDraft));

        PublicationUseCase.ExportRequest request = new PublicationUseCase.ExportRequest("2026-W18", "json");
        byte[] result = publicationService.exportDrafts(request);

        assertNotNull(result);
        String json = new String(result);
        assertTrue(json.contains("\"id\": \"draft-123\""));
        assertTrue(json.contains("\"channel\": \"LINKEDIN\""));
    }

    @Test
    void exportDrafts_shouldThrowExceptionWhenNoDraftsFound() {
        when(draftRepository.findByStatus(Draft.DraftStatus.APPROVED)).thenReturn(List.of());

        PublicationUseCase.ExportRequest request = new PublicationUseCase.ExportRequest("2026-W18", "csv");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                publicationService.exportDrafts(request)
        );

        assertEquals("No approved drafts found for export", exception.getMessage());
    }
}
