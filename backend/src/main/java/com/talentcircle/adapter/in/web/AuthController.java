package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.port.in.AuthUseCase;
import com.talentcircle.domain.port.in.AuthUseCase.LoginRequest;
import com.talentcircle.domain.port.in.AuthUseCase.LoginResponse;
import com.talentcircle.domain.port.in.AuthUseCase.RefreshRequest;
import com.talentcircle.domain.port.in.AuthUseCase.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @Operation(summary = "Iniciar sesion", description = "Autentica al usuario con email y contrasena")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Credenciales invalidas")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authUseCase.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Registrar usuario", description = "Crea un nuevo usuario. Roles validos: ADMIN, EDITOR")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "409", description = "El email ya esta registrado")
    })
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

    @Operation(summary = "Renovar token", description = "Genera un nuevo par de tokens usando un refresh token valido")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens renovados exitosamente"),
        @ApiResponse(responseCode = "401", description = "Refresh token invalido o expirado")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        LoginResponse response = authUseCase.refresh(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cerrar sesion", description = "Revoca el refresh token del usuario autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sesion cerrada exitosamente"),
        @ApiResponse(responseCode = "401", description = "Token no proporcionado o invalido")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            authUseCase.logout(authentication.getName());
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cambiar contrasena", description = "Cambia la contrasena del usuario autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Contrasena actualizada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Contrasena actual incorrecta"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request) {
        authUseCase.changePassword(authentication.getName(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "Datos para registrar un nuevo usuario")
    public record RegisterRequest(
            @Schema(description = "Email unico del usuario") String email,
            @Schema(description = "Contrasena") String password,
            @Schema(description = "Nombre completo") String fullName,
            @Schema(description = "Rol del usuario. Valores: ADMIN, EDITOR") String role
    ) {}

    @Schema(description = "Datos para cambiar contrasena")
    public record ChangePasswordRequest(
            @Schema(description = "Contrasena actual") String currentPassword,
            @Schema(description = "Nueva contrasena") String newPassword
    ) {}
}
