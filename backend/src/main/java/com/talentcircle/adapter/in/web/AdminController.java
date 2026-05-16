package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.port.in.AdminUseCase;
import com.talentcircle.domain.port.in.AdminUseCase.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminUseCase adminUseCase;

    public AdminController(AdminUseCase adminUseCase) {
        this.adminUseCase = adminUseCase;
    }

    // =========================================================================
    // USUARIOS
    // =========================================================================

    @Tag(name = "Admin › Usuarios")
    @Operation(
        summary = "Listar usuarios",
        description = "Retorna todos los usuarios registrados en el sistema con su rol y estado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de usuarios",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    [{"id": "550e8400-e29b-41d4-a716-446655440000", "email": "admin@talentcircle.com", "fullName": "Admin TalentCircle", "role": "ADMIN", "active": true}, {"id": "660e8400-e29b-41d4-a716-446655440001", "email": "editor@talentcircle.com", "fullName": "Editor TalentCircle", "role": "EDITOR", "active": true}]
                    """))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos de ADMIN")
    })
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(adminUseCase.getUsers());
    }

    @Tag(name = "Admin › Usuarios")
    @Operation(
        summary = "Crear usuario",
        description = "Crea un nuevo usuario con rol `ADMIN` o `EDITOR`. Requiere autenticación con rol ADMIN."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario creado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"id": "770e8400-e29b-41d4-a716-446655440002", "email": "editor2@talentcircle.com", "fullName": "Editor Dos", "role": "EDITOR", "active": true}
                    """))),
        @ApiResponse(responseCode = "409", description = "Email ya registrado")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(examples = @ExampleObject(value = """
            {"email": "editor2@talentcircle.com", "password": "Editor123!", "fullName": "Editor Dos", "role": "EDITOR"}
            """))
    )
    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(adminUseCase.createUser(request));
    }

    @Tag(name = "Admin › Usuarios")
    @Operation(
        summary = "Actualizar usuario",
        description = "Actualiza nombre, rol o estado activo de un usuario. Solo se actualizan los campos enviados (no nulos)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(examples = @ExampleObject(value = """
            {"fullName": "Editor Actualizado", "role": "EDITOR", "active": true}
            """))
    )
    @PutMapping("/users/{id}")
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "UUID del usuario", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(adminUseCase.updateUser(id, request));
    }

    // =========================================================================
    // FUENTES COMUNITARIAS
    // =========================================================================

    @Tag(name = "Admin › Fuentes")
    @Operation(
        summary = "Listar fuentes activas",
        description = "Retorna todas las fuentes comunitarias activas (Discord, Circle, Slack)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de fuentes",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    [{"id": "src-001", "name": "Discord TalentCircle", "type": "DISCORD", "active": true}]
                    """)))
    })
    @GetMapping("/sources")
    public ResponseEntity<List<SourceDto>> getSources() {
        return ResponseEntity.ok(adminUseCase.getSources());
    }

    @Tag(name = "Admin › Fuentes")
    @Operation(
        summary = "Crear fuente comunitaria",
        description = "Registra una nueva fuente. Tipos válidos: `DISCORD`, `CIRCLE`, `SLACK`. " +
                      "La `apiKey` se almacena cifrada con AES-256."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fuente creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(examples = @ExampleObject(value = """
            {"name": "Discord TalentCircle", "type": "DISCORD", "apiUrl": "https://discord.com/api/v10", "apiKey": "Bot TOKEN_AQUI"}
            """))
    )
    @PostMapping("/sources")
    public ResponseEntity<SourceDto> createSource(@RequestBody CreateSourceRequest request) {
        return ResponseEntity.ok(adminUseCase.createSource(request));
    }

    @Tag(name = "Admin › Fuentes")
    @Operation(
        summary = "Actualizar fuente comunitaria",
        description = "Actualiza los datos de una fuente existente. Solo se actualizan los campos no nulos."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fuente actualizada"),
        @ApiResponse(responseCode = "404", description = "Fuente no encontrada")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(examples = @ExampleObject(value = """
            {"name": "Discord TalentCircle v2", "apiUrl": "https://discord.com/api/v10", "apiKey": "Bot NUEVO_TOKEN", "active": true}
            """))
    )
    @PutMapping("/sources/{id}")
    public ResponseEntity<SourceDto> updateSource(
            @Parameter(description = "UUID de la fuente", example = "src-001")
            @PathVariable String id,
            @RequestBody UpdateSourceRequest request) {
        return ResponseEntity.ok(adminUseCase.updateSource(id, request));
    }

    @Tag(name = "Admin › Fuentes")
    @Operation(
        summary = "Eliminar fuente (soft delete)",
        description = "Desactiva una fuente comunitaria. No elimina los datos históricos recolectados."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Fuente desactivada"),
        @ApiResponse(responseCode = "404", description = "Fuente no encontrada")
    })
    @DeleteMapping("/sources/{id}")
    public ResponseEntity<Void> deleteSource(
            @Parameter(description = "UUID de la fuente", example = "src-001")
            @PathVariable String id) {
        adminUseCase.deleteSource(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // CONFIGURACIÓN DEL PIPELINE
    // =========================================================================

    @Tag(name = "Admin › Config")
    @Operation(
        summary = "Obtener configuración del pipeline",
        description = "Retorna la configuración actual: proveedor LLM, modelo, prompts por canal, " +
                      "máximo de ítems y cron de ejecución."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Configuración actual",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"llmProvider": "openai", "llmModel": "gpt-4o", "newsletterPrompt": "Genera un newsletter en español sobre los temas más relevantes...", "linkedinPrompt": "Crea un post de LinkedIn profesional...", "twitterPrompt": "Crea un tweet conciso de máximo 280 caracteres...", "maxItemsPerChannel": 10, "scheduleCron": "0 0 18 * * FRI"}
                    """)))
    })
    @GetMapping("/config")
    public ResponseEntity<ConfigDto> getConfig() {
        return ResponseEntity.ok(adminUseCase.getConfig());
    }

    @Tag(name = "Admin › Config")
    @Operation(
        summary = "Actualizar configuración del pipeline",
        description = "Actualiza la configuración del pipeline. Solo se actualizan los campos enviados. " +
                      "El `scheduleCron` usa formato Spring Cron (6 campos). " +
                      "Proveedores LLM válidos: `openai`, `anthropic`."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Configuración actualizada"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(examples = @ExampleObject(value = """
            {"llmProvider": "openai", "llmModel": "gpt-4o", "newsletterPrompt": "Genera un newsletter semanal en español...", "linkedinPrompt": "Crea un post de LinkedIn de 150-300 palabras...", "twitterPrompt": "Crea un tweet de máximo 280 caracteres...", "maxItemsPerChannel": 10, "scheduleCron": "0 0 18 * * FRI"}
            """))
    )
    @PutMapping("/config")
    public ResponseEntity<ConfigDto> updateConfig(@RequestBody UpdateConfigRequest request) {
        return ResponseEntity.ok(adminUseCase.updateConfig(request));
    }

    // =========================================================================
    // EJECUCIONES DEL PIPELINE
    // =========================================================================

    @Tag(name = "Admin › Ejecuciones")
    @Operation(
        summary = "Listar ejecuciones",
        description = "Retorna el historial de ejecuciones del pipeline con su estado y fechas."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de ejecuciones",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    [{"id": "exec-001", "weekStart": "2026-04-28", "weekEnd": "2026-05-02", "status": "COMPLETED", "startedAt": "2026-05-02T18:00:00", "completedAt": "2026-05-02T18:15:32"}]
                    """)))
    })
    @GetMapping("/executions")
    public ResponseEntity<List<ExecutionSummaryDto>> getExecutions() {
        return ResponseEntity.ok(adminUseCase.getExecutions());
    }

    @Tag(name = "Admin › Ejecuciones")
    @Operation(
        summary = "Detalle de una ejecución",
        description = "Retorna el detalle completo de una ejecución específica del pipeline."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle de la ejecución"),
        @ApiResponse(responseCode = "404", description = "Ejecución no encontrada")
    })
    @GetMapping("/executions/{id}")
    public ResponseEntity<ExecutionSummaryDto> getExecutionDetail(
            @Parameter(description = "UUID de la ejecución", example = "exec-001")
            @PathVariable String id) {
        return ResponseEntity.ok(adminUseCase.getExecutionDetail(id));
    }

    @Tag(name = "Admin › Ejecuciones")
    @Operation(
        summary = "Disparar ejecución manual",
        description = "Inicia el pipeline manualmente fuera del horario programado. " +
                      "El pipeline ejecuta: recolección → análisis IA → generación de borradores."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Ejecución iniciada en background"),
        @ApiResponse(responseCode = "400", description = "Ya hay una ejecución en curso")
    })
    @PostMapping("/executions/trigger")
    public ResponseEntity<Void> triggerExecution(
            @Parameter(description = "Identificador de quien dispara la ejecución", example = "admin@talentcircle.com")
            @RequestParam String triggeredBy) {
        adminUseCase.triggerExecution(triggeredBy);
        return ResponseEntity.accepted().build();
    }
}
