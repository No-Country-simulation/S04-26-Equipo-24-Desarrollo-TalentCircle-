package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.port.in.CommunityCollectorUseCase;
import com.talentcircle.domain.port.in.CommunityCollectorUseCase.CommunityActivityDto;
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
@RequestMapping("/api/v1/admin/collector")
@Tag(name = "Collector")
@SecurityRequirement(name = "bearerAuth")
public class CommunityCollectorController {

    private final CommunityCollectorUseCase collectorUseCase;

    public CommunityCollectorController(CommunityCollectorUseCase collectorUseCase) {
        this.collectorUseCase = collectorUseCase;
    }

    @Operation(
        summary = "Disparar recolección manual",
        description = "Inicia la recolección de actividad comunitaria para una fuente específica " +
                      "dentro de una ejecución del pipeline. Útil para testing y depuración. " +
                      "La recolección es asíncrona — retorna 202 inmediatamente."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Recolección iniciada en background"),
        @ApiResponse(responseCode = "404", description = "Ejecución o fuente no encontrada"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PostMapping("/collect")
    public ResponseEntity<Void> triggerCollection(
            @Parameter(description = "UUID de la ejecución semanal", example = "exec-001", required = true)
            @RequestParam String executionId,

            @Parameter(description = "UUID de la fuente comunitaria", example = "src-001", required = true)
            @RequestParam String sourceId) {
        collectorUseCase.collectActivity(executionId, sourceId);
        return ResponseEntity.accepted().build();
    }

    @Operation(
        summary = "Listar actividades recolectadas",
        description = "Retorna todas las actividades comunitarias recolectadas para una ejecución específica. " +
                      "Incluye posts, preguntas y recursos con sus métricas de engagement."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de actividades recolectadas",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    [{"id": "act-001", "title": "¿Cómo mejorar tu perfil de LinkedIn?", "content": "Tips que me funcionaron...", "type": "POST", "reactionCount": 47, "responseCount": 23, "shareCount": 12, "author": "María García", "sourceUrl": "https://discord.com/channels/123/456/789"}, {"id": "act-002", "title": "Guía completa de entrevistas técnicas 2026", "content": "Comparto esta guía que encontré muy útil...", "type": "RESOURCE", "reactionCount": 89, "responseCount": 5, "shareCount": 34, "author": "Carlos López", "sourceUrl": "https://discord.com/channels/123/456/790"}]
                    """))),
        @ApiResponse(responseCode = "404", description = "Ejecución no encontrada")
    })
    @GetMapping("/activities")
    public ResponseEntity<List<CommunityActivityDto>> getActivities(
            @Parameter(description = "UUID de la ejecución semanal", example = "exec-001", required = true)
            @RequestParam String executionId) {
        return ResponseEntity.ok(collectorUseCase.getActivitiesByExecution(executionId));
    }
}
