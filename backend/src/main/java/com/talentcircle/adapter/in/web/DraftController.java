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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Drafts", description = "Gestión del panel editorial de borradores")
@SecurityRequirement(name = "bearerAuth")
public class DraftController {

    private final DraftReviewUseCase draftReviewUseCase;
    private final PublicationUseCase publicationUseCase;

    public DraftController(DraftReviewUseCase draftReviewUseCase,
                           PublicationUseCase publicationUseCase) {
        this.draftReviewUseCase = draftReviewUseCase;
        this.publicationUseCase = publicationUseCase;
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/drafts — RF-17: Listado paginado con filtros
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Listar borradores",
        description = "Retorna borradores con filtros opcionales. " +
                      "Canales válidos: `NEWSLETTER`, `LINKEDIN`, `TWITTER`. " +
                      "Estados válidos: `PENDING`, `APPROVED`, `REJECTED`, `PUBLISHED`."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de borradores",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    [
                      {
                        "id": "draft-001",
                        "channel": "NEWSLETTER",
                        "status": "PENDING",
                        "createdAt": "2026-05-02T18:15:00",
                        "summary": "Resumen de la semana: los temas más relevantes..."
                      }
                    ]
                    """))),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<List<DraftSummaryDto>> listDrafts(
            @Parameter(description = "Filtrar por canal: NEWSLETTER, LINKEDIN, TWITTER", example = "NEWSLETTER")
            @RequestParam(required = false) String channel,

            @Parameter(description = "Filtrar por estado: PENDING, APPROVED, REJECTED, PUBLISHED", example = "PENDING")
            @RequestParam(required = false) String status,

            @Parameter(description = "Fecha inicio de semana (ISO 8601)", example = "2026-04-28")
            @RequestParam(required = false) String weekStart,

            @Parameter(description = "Fecha fin de semana (ISO 8601)", example = "2026-05-02")
            @RequestParam(required = false) String weekEnd,

            @Parameter(description = "Número de página (base 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamaño de página", example = "50")
            @RequestParam(defaultValue = "50") int size) {

        return ResponseEntity.ok(
                draftReviewUseCase.listDrafts(channel, status, weekStart, weekEnd, page, size));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/drafts/{id} — RF-18: Vista detalle con fuentes y versiones
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Detalle de un borrador",
        description = "Retorna el contenido completo del borrador, incluyendo actividades fuente " +
                      "con su puntuación de relevancia y el historial de versiones editadas."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle del borrador"),
        @ApiResponse(responseCode = "404", description = "Borrador no encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<DraftDetailDto> getDraftDetail(
            @Parameter(description = "UUID del borrador", example = "draft-001")
            @PathVariable String id) {

        return ResponseEntity.ok(draftReviewUseCase.getDraftDetail(id));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/drafts/{id}/content — RF-19: Edición inline
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Editar contenido del borrador",
        description = "Actualiza el contenido editado. Cada edición crea automáticamente " +
                      "una nueva versión en el historial. El contenido original de IA se preserva."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Borrador actualizado, nueva versión creada"),
        @ApiResponse(responseCode = "404", description = "Borrador no encontrado")
    })
    @PatchMapping("/{id}/content")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<DraftDetailDto> updateContent(
            @Parameter(description = "UUID del borrador", example = "draft-001")
            @PathVariable String id,
            @RequestBody UpdateContentRequest request) {

        return ResponseEntity.ok(draftReviewUseCase.updateContent(id, request));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/drafts/{id}/approve — RF-20: Aprobación
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Aprobar borrador",
        description = "Cambia el estado a `APPROVED`. Solo borradores en estado `PENDING` pueden aprobarse."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Borrador aprobado"),
        @ApiResponse(responseCode = "404", description = "Borrador no encontrado"),
        @ApiResponse(responseCode = "409", description = "El borrador no está en estado PENDING")
    })
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<DraftDetailDto> approveDraft(
            @Parameter(description = "UUID del borrador", example = "draft-001")
            @PathVariable String id,
            @RequestBody(required = false) ApproveRequest request) {

        return ResponseEntity.ok(draftReviewUseCase.approveDraft(id));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/drafts/{id}/reject — RF-20: Rechazo con comentario
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Rechazar borrador",
        description = "Cambia el estado a `REJECTED`. Se requiere un motivo de rechazo obligatorio."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Borrador rechazado"),
        @ApiResponse(responseCode = "400", description = "Motivo de rechazo requerido"),
        @ApiResponse(responseCode = "404", description = "Borrador no encontrado")
    })
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<DraftDetailDto> rejectDraft(
            @Parameter(description = "UUID del borrador", example = "draft-001")
            @PathVariable String id,
            @RequestBody RejectRequest request) {

        return ResponseEntity.ok(draftReviewUseCase.rejectDraft(id, request));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/drafts/{id}/publish — RF-23: Publicación en LinkedIn
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Publicar borrador en LinkedIn",
        description = "Publica el borrador aprobado directamente en LinkedIn via API v2. " +
                      "Solo borradores con estado `APPROVED` y canal `LINKEDIN` pueden publicarse."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Publicación exitosa o registrada con error"),
        @ApiResponse(responseCode = "400", description = "El borrador no está aprobado"),
        @ApiResponse(responseCode = "404", description = "Borrador no encontrado")
    })
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<PublicationDto> publishDraft(
            @Parameter(description = "UUID del borrador", example = "draft-001")
            @PathVariable String id) {

        return ResponseEntity.ok(publicationUseCase.publishDraft(id));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/drafts/export — RF-24/RF-25: Exportación JSON o CSV
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Exportar borradores aprobados",
        description = "Genera un archivo descargable con todos los borradores `APPROVED`. " +
                      "Formatos soportados: `csv` (default), `json`."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Archivo generado para descarga"),
        @ApiResponse(responseCode = "404", description = "No hay borradores aprobados para exportar")
    })
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<byte[]> exportDrafts(
            @Parameter(description = "Formato de exportación: csv o json", example = "csv")
            @RequestParam(defaultValue = "csv") String format,

            @Parameter(description = "Semana a exportar (ej: 2026-W18)", example = "2026-W18")
            @RequestParam(required = false) String week) {

        byte[] data = publicationUseCase.exportDrafts(new ExportRequest(format, week));

        String contentType = "json".equalsIgnoreCase(format) ? "application/json" : "text/csv";
        String filename = "drafts-export." + format.toLowerCase();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
