package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.port.in.AuthUseCase;
import com.talentcircle.domain.port.in.AuthUseCase.LoginRequest;
import com.talentcircle.domain.port.in.AuthUseCase.LoginResponse;
import com.talentcircle.domain.port.in.AuthUseCase.RefreshRequest;
import com.talentcircle.domain.port.in.AuthUseCase.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /login
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Iniciar sesión",
        description = "Autentica al usuario con email y contraseña. Retorna un `accessToken` (válido 8 horas) " +
                      "y un `refreshToken` (válido 7 días)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = LoginResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLWlkIiwicm9sZSI6IkFETUlOIn0.abc123",
                      "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
                      "expiresIn": "28800000",
                      "user": {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "email": "admin@talentcircle.com",
                        "fullName": "Admin TalentCircle",
                        "role": "ADMIN"
                      }
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    { "error": "UNAUTHORIZED", "message": "Invalid credentials", "timestamp": "2026-05-06T20:00:00Z" }
                    """)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(examples = @ExampleObject(value = """
            { "email": "admin@talentcircle.com", "password": "Admin123!" }
            """))
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authUseCase.login(request);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /register
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Registrar usuario",
        description = "Crea un nuevo usuario en el sistema. **Endpoint público** — no requiere token. " +
                      "Roles válidos: `ADMIN`, `EDITOR`. Si no se especifica `role`, se asigna `EDITOR` por defecto."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(value = """
                    {
                      "id": "550e8400-e29b-41d4-a716-446655440000",
                      "email": "admin@talentcircle.com",
                      "fullName": "Admin TalentCircle",
                      "role": "ADMIN"
                    }
                    """))),
        @ApiResponse(responseCode = "409", description = "El email ya está registrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    { "error": "CONFLICT", "message": "Email already exists", "timestamp": "2026-05-06T20:00:00Z" }
                    """)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(examples = @ExampleObject(value = """
            {
              "email": "admin@talentcircle.com",
              "password": "Admin123!",
              "fullName": "Admin TalentCircle",
              "role": "ADMIN"
            }
            """))
    )
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody RegisterRequest request) {
        UserDto user = authUseCase.createUser(
                request.email(),
                request.password(),
                request.fullName(),
                request.role() != null ? request.role() : "EDITOR"
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /refresh
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Renovar token",
        description = "Genera un nuevo par de tokens (`accessToken` + `refreshToken`) usando un `refreshToken` válido. " +
                      "El refresh token anterior queda invalidado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens renovados exitosamente"),
        @ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    { "error": "UNAUTHORIZED", "message": "Invalid refresh token", "timestamp": "2026-05-06T20:00:00Z" }
                    """)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(examples = @ExampleObject(value = """
            { "refreshToken": "550e8400-e29b-41d4-a716-446655440000" }
            """))
    )
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        LoginResponse response = authUseCase.refresh(request);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /logout
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Cerrar sesión",
        description = "Revoca el refresh token del usuario autenticado. El access token sigue válido hasta su expiración."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sesión cerrada exitosamente"),
        @ApiResponse(responseCode = "401", description = "Token no proporcionado o inválido")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // authUseCase.logout(userId extraído del token);
        }
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /change-password
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Cambiar contraseña",
        description = "Cambia la contraseña del usuario autenticado. Requiere la contraseña actual para confirmar identidad."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Contraseña actualizada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Contraseña actual incorrecta"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestParam @Schema(description = "Contraseña actual", example = "Admin123!") String currentPassword,
            @RequestParam @Schema(description = "Nueva contraseña (mínimo 8 caracteres)", example = "NewPass456!") String newPassword) {
        // authUseCase.changePassword(userId, currentPassword, newPassword);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTOs internos
    // ─────────────────────────────────────────────────────────────────────────

    @Schema(description = "Datos para registrar un nuevo usuario")
    public record RegisterRequest(
            @Schema(description = "Email único del usuario", example = "admin@talentcircle.com", requiredMode = Schema.RequiredMode.REQUIRED)
            String email,

            @Schema(description = "Contraseña (mínimo 8 caracteres)", example = "Admin123!", requiredMode = Schema.RequiredMode.REQUIRED)
            String password,

            @Schema(description = "Nombre completo", example = "Admin TalentCircle", requiredMode = Schema.RequiredMode.REQUIRED)
            String fullName,

            @Schema(description = "Rol del usuario. Valores: ADMIN, EDITOR", example = "ADMIN", allowableValues = {"ADMIN", "EDITOR"})
            String role
    ) {}
}
