package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.port.in.DraftReviewUseCase;
import com.talentcircle.domain.port.in.DraftReviewUseCase.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drafts")
@Tag(name = "Drafts")
@SecurityRequirement(name = "bearerAuth")
public class DraftController {

    private final DraftReviewUseCase draftReviewUseCase;

    public DraftController(DraftReviewUseCase draftReviewUseCase) {
        this.draftReviewUseCase = draftReviewUseCase;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /drafts
    // ─────────────────────────────────────────────────────────────────────────

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
                        "summary": "Resumen de la semana: los temas más relevantes de la comunidad..."
                      },
                      {
                        "id": "draft-002",
                        "channel": "LINKEDIN",
                        "status": "APPROVED",
                        "createdAt": "2026-05-02T18:15:00",
                        "summary": "Esta semana en TalentCircle destacamos..."
                      }
                    ]
                    """))),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @GetMapping
    public ResponseEntity<List<DraftSummaryDto>> listDrafts(
            @Parameter(description = "Filtrar por canal. Valores: NEWSLETTER, LINKEDIN, TWITTER", example = "NEWSLETTER")
            @RequestParam(required = false) String channel,

            @Parameter(description = "Filtrar por estado. Valores: PENDING, APPROVED, REJECTED, PUBLISHED", example = "PENDING")
            @RequestParam(required = false) String status,

            @Parameter(description = "Fecha inicio de semana (ISO 8601)", example = "2026-04-28")
            @RequestParam(required = false) String weekStart,

            @Parameter(description = "Fecha fin de semana (ISO 8601)", example = "2026-05-02")
            @RequestParam(required = false) String weekEnd,

            @Parameter(description = "Número de página (base 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamaño de página (máx 100)", example = "50")
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(draftReviewUseCase.listDrafts(channel, status, weekStart, weekEnd, page, size));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /drafts/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Detalle de un borrador",
        description = "Retorna el contenido completo del borrador, incluyendo las actividades fuente " +
                      "con su puntuación de relevancia y el historial de versiones editadas."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle del borrador",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {
                      "id": "draft-001",
                      "channel": "NEWSLETTER",
                      "content": "# Newsletter Semanal\\n\\nEsta semana en TalentCircle...",
                      "editedContent": null,
                      "status": "PENDING",
                      "aiScore": 8.5,
                      "createdAt": "2026-05-02T18:15:00",
                      "updatedAt": "2026-05-02T18:15:00",
                      "sources": [
                        {
                          "id": "src-act-001",
                          "title": "¿Cómo mejorar tu perfil de LinkedIn?",
                          "relevanceScore": 9.2
                        }
                      ],
                      "versions": [
                        {
                          "id": "ver-001",
                          "content": "Versión original generada por IA...",
                          "editedBy": null,
                          "editedAt": "2026-05-02T18:15:00",
                          "versionNumber": 1
                        }
                      ]
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Borrador no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DraftDetailDto> getDraftDetail(
            @Parameter(description = "UUID del borrador", example = "draft-001")
            @PathVariable String id) {
        return ResponseEntity.ok(draftReviewUseCase.getDraftDetail(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /drafts/{id}/content
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Editar contenido del borrador",
        description = "Actualiza el contenido editado del borrador. Cada edición crea automáticamente " +
                      "una nueva versión en el historial. El contenido original generado por IA se preserva."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Borrador actualizado con nueva versión creada"),
        @ApiResponse(responseCode = "404", description = "Borrador no encontrado"),
        @ApiResponse(responseCode = "409", description = "No se puede editar un borrador PUBLISHED")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(examples = @ExampleObject(value = """
            {
              "content": "# Newsletter Semanal — Edición del Editor\\n\\nEsta semana destacamos..."
            }
            """))
    )
    @PatchMapping("/{id}/content")
    public ResponseEntity<DraftDetailDto> updateContent(
            @Parameter(description = "UUID del borrador", example = "draft-001")
            @PathVariable String id,
            @RequestBody UpdateContentRequest request) {
        return ResponseEntity.ok(draftReviewUseCase.updateContent(id, request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /drafts/{id}/approve
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Aprobar borrador",
        description = "Cambia el estado del borrador a `APPROVED`. Solo borradores en estado `PENDING` " +
                      "pueden ser aprobados. Los borradores aprobados quedan listos para publicación."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Borrador aprobado. Estado cambia a APPROVED"),
        @ApiResponse(responseCode = "404", description = "Borrador no encontrado"),
        @ApiResponse(responseCode = "409", description = "El borrador no está en estado PENDING")
    })
    @PostMapping("/{id}/approve")
    public ResponseEntity<DraftDetailDto> approveDraft(
            @Parameter(description = "UUID del borrador", example = "draft-001")
            @PathVariable String id,
            @RequestBody(required = false) ApproveRequest request) {
        return ResponseEntity.ok(draftReviewUseCase.approveDraft(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /drafts/{id}/reject
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Rechazar borrador",
        description = "Cambia el estado del borrador a `REJECTED`. Se requiere un motivo de rechazo. " +
                      "El borrador puede ser regenerado en la siguiente ejecución del pipeline."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Borrador rechazado. Estado cambia a REJECTED"),
        @ApiResponse(responseCode = "400", description = "Motivo de rechazo requerido"),
        @ApiResponse(responseCode = "404", description = "Borrador no encontrado")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(examples = @ExampleObject(value = """
            {
              "reason": "El contenido no refleja correctamente los temas de la semana. Regenerar con más contexto."
            }
            """))
    )
    @PostMapping("/{id}/reject")
    public ResponseEntity<DraftDetailDto> rejectDraft(
            @Parameter(description = "UUID del borrador", example = "draft-001")
            @PathVariable String id,
            @RequestBody RejectRequest request) {
        return ResponseEntity.ok(draftReviewUseCase.rejectDraft(id, request));
    }
}
