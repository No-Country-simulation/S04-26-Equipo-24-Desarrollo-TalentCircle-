package com.talentcircle.application.service;

import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.Publication;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.LinkedInClientPort;
import com.talentcircle.domain.port.out.PublicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para PublicationService.
 *
 * Incluye el test de propiedad de idempotencia (Tarea 10.B.2):
 *   Propiedad: publicar un borrador que NO está en estado APPROVED
 *              lanza IllegalStateException sin llamar a LinkedIn API.
 */
@ExtendWith(MockitoExtension.class)
class PublicationServiceTest {

    @Mock private DraftRepository draftRepository;
    @Mock private PublicationRepository publicationRepository;
    @Mock private LinkedInClientPort linkedInClient;

    private PublicationService service;

    @BeforeEach
    void setUp() {
        service = new PublicationService(draftRepository, publicationRepository, linkedInClient);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test de propiedad: idempotencia — solo APPROVED puede publicarse
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Propiedad: publicar draft PUBLISHED lanza IllegalStateException sin llamar a LinkedIn")
    void publishDraft_alreadyPublished_throwsWithoutCallingLinkedIn() {
        Draft draft = buildDraft(Draft.DraftStatus.PUBLISHED);
        when(draftRepository.findById("draft-1")).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.publishDraft("draft-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED");

        // Propiedad: LinkedIn API nunca se llama si el draft no está APPROVED
        verifyNoInteractions(linkedInClient);
        verifyNoInteractions(publicationRepository);
    }

    @Test
    @DisplayName("Propiedad: publicar draft PENDING lanza IllegalStateException sin llamar a LinkedIn")
    void publishDraft_pending_throwsWithoutCallingLinkedIn() {
        Draft draft = buildDraft(Draft.DraftStatus.PENDING);
        when(draftRepository.findById("draft-2")).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.publishDraft("draft-2"))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(linkedInClient);
    }

    @Test
    @DisplayName("Propiedad: publicar draft REJECTED lanza IllegalStateException sin llamar a LinkedIn")
    void publishDraft_rejected_throwsWithoutCallingLinkedIn() {
        Draft draft = buildDraft(Draft.DraftStatus.REJECTED);
        when(draftRepository.findById("draft-3")).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.publishDraft("draft-3"))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(linkedInClient);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Flujo exitoso: APPROVED → PUBLISHED
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("publishDraft() con draft APPROVED llama a LinkedIn y devuelve SUCCESS")
    void publishDraft_approved_callsLinkedInAndReturnsSuccess() {
        Draft draft = buildDraft(Draft.DraftStatus.APPROVED);
        draft.setChannel(Draft.Channel.LINKEDIN);
        draft.setContent("Contenido del post de LinkedIn");

        Publication savedPublication = new Publication();
        savedPublication.setId("pub-1");
        savedPublication.setDraft(draft);
        savedPublication.setStatus(Publication.PublicationStatus.SUCCESS);
        savedPublication.setExternalPostId("urn:li:ugcPost:999");

        when(draftRepository.findById("draft-approved")).thenReturn(Optional.of(draft));
        when(linkedInClient.publishPost(any())).thenReturn("urn:li:ugcPost:999");
        when(publicationRepository.save(any())).thenReturn(savedPublication);
        when(draftRepository.save(any())).thenReturn(draft);

        var result = service.publishDraft("draft-approved");

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.externalPostId()).isEqualTo("urn:li:ugcPost:999");
        verify(linkedInClient).publishPost("Contenido del post de LinkedIn");
    }

    @Test
    @DisplayName("publishDraft() usa editedContent si existe, no el content original")
    void publishDraft_usesEditedContentWhenPresent() {
        Draft draft = buildDraft(Draft.DraftStatus.APPROVED);
        draft.setChannel(Draft.Channel.LINKEDIN);
        draft.setContent("Contenido original de IA");
        draft.setEditedContent("Contenido editado por el editor");

        Publication savedPublication = new Publication();
        savedPublication.setId("pub-2");
        savedPublication.setDraft(draft);
        savedPublication.setStatus(Publication.PublicationStatus.SUCCESS);
        savedPublication.setExternalPostId("urn:li:ugcPost:888");

        when(draftRepository.findById("draft-edited")).thenReturn(Optional.of(draft));
        when(linkedInClient.publishPost(any())).thenReturn("urn:li:ugcPost:888");
        when(publicationRepository.save(any())).thenReturn(savedPublication);
        when(draftRepository.save(any())).thenReturn(draft);

        service.publishDraft("draft-edited");

        // Debe usar el contenido editado, no el original
        verify(linkedInClient).publishPost("Contenido editado por el editor");
        verify(linkedInClient, never()).publishPost("Contenido original de IA");
    }

    @Test
    @DisplayName("publishDraft() cuando LinkedIn falla guarda Publication con status FAILED")
    void publishDraft_linkedInFails_savesFailedPublication() {
        Draft draft = buildDraft(Draft.DraftStatus.APPROVED);
        draft.setChannel(Draft.Channel.LINKEDIN);
        draft.setContent("Contenido");

        Publication failedPublication = new Publication();
        failedPublication.setId("pub-fail");
        failedPublication.setDraft(draft);
        failedPublication.setStatus(Publication.PublicationStatus.FAILED);
        failedPublication.setErrorMessage("LinkedIn API error");

        when(draftRepository.findById("draft-fail")).thenReturn(Optional.of(draft));
        when(linkedInClient.publishPost(any())).thenThrow(new RuntimeException("LinkedIn API error"));
        when(publicationRepository.save(any())).thenReturn(failedPublication);

        var result = service.publishDraft("draft-fail");

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.errorMessage()).isEqualTo("LinkedIn API error");
        // El draft NO debe cambiar a PUBLISHED cuando falla
        verify(draftRepository, never()).save(any());
    }

    @Test
    @DisplayName("publishDraft() lanza IllegalArgumentException si el draft no existe")
    void publishDraft_draftNotFound_throwsIllegalArgument() {
        when(draftRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publishDraft("no-existe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no-existe");

        verifyNoInteractions(linkedInClient);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Draft buildDraft(Draft.DraftStatus status) {
        Draft draft = new Draft();
        draft.setId("draft-" + status.name().toLowerCase());
        draft.setStatus(status);
        draft.setChannel(Draft.Channel.LINKEDIN);
        draft.setContent("Contenido de prueba");
        return draft;
    }
}
