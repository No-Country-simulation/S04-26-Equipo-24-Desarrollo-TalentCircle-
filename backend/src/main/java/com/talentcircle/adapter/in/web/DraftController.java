package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.port.in.DraftReviewUseCase;
import com.talentcircle.domain.port.in.DraftReviewUseCase.ApproveRequest;
import com.talentcircle.domain.port.in.DraftReviewUseCase.DraftDetailDto;
import com.talentcircle.domain.port.in.DraftReviewUseCase.DraftSummaryDto;
import com.talentcircle.domain.port.in.DraftReviewUseCase.RejectRequest;
import com.talentcircle.domain.port.in.DraftReviewUseCase.UpdateContentRequest;
import com.talentcircle.domain.port.in.PublicationUseCase;
import com.talentcircle.domain.port.in.PublicationUseCase.ExportRequest;
import com.talentcircle.domain.port.in.PublicationUseCase.PublicationDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for draft management.
 * RF-17 to RF-26: Panel Editorial + Publicación y Exportación.
 */
@RestController
@RequestMapping("/api/v1/drafts")
public class DraftController {

    private final DraftReviewUseCase draftReviewUseCase;
    private final PublicationUseCase publicationUseCase;

    public DraftController(DraftReviewUseCase draftReviewUseCase,
                           PublicationUseCase publicationUseCase) {
        this.draftReviewUseCase = draftReviewUseCase;
        this.publicationUseCase = publicationUseCase;
    }

    // RF-17: Listado de borradores con filtros y paginación
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<List<DraftSummaryDto>> listDrafts(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String weekStart,
            @RequestParam(required = false) String weekEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(draftReviewUseCase.listDrafts(channel, status, weekStart, weekEnd, page, size));
    }

    // RF-18: Vista detalle con fuentes y versiones
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<DraftDetailDto> getDraftDetail(@PathVariable String id) {
        return ResponseEntity.ok(draftReviewUseCase.getDraftDetail(id));
    }

    // RF-19: Edición inline
    @PatchMapping("/{id}/content")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<DraftDetailDto> updateContent(@PathVariable String id,
                                                        @RequestBody UpdateContentRequest request) {
        return ResponseEntity.ok(draftReviewUseCase.updateContent(id, request));
    }

    // RF-20: Aprobación
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<DraftDetailDto> approveDraft(@PathVariable String id,
                                                       @RequestBody(required = false) ApproveRequest request) {
        return ResponseEntity.ok(draftReviewUseCase.approveDraft(id));
    }

    // RF-20: Rechazo con comentario obligatorio
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<DraftDetailDto> rejectDraft(@PathVariable String id,
                                                      @RequestBody RejectRequest request) {
        return ResponseEntity.ok(draftReviewUseCase.rejectDraft(id, request));
    }

    // RF-23: Publicación en LinkedIn
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<PublicationDto> publishDraft(@PathVariable String id) {
        return ResponseEntity.ok(publicationUseCase.publishDraft(id));
    }

    // RF-24 / RF-25: Exportación JSON o CSV
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<byte[]> exportDrafts(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String week) {

        byte[] data = publicationUseCase.exportDrafts(new ExportRequest(format, week));

        String contentType = "csv".equalsIgnoreCase(format) ? "text/csv" : "application/json";
        String filename = "drafts-export." + format.toLowerCase();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
