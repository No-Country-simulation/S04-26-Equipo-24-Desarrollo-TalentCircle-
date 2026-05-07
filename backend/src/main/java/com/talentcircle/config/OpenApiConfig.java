package com.talentcircle.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TalentCircle Content Pipeline API")
                        .description("""
                                ## Pipeline de contenido comunitario automatizado

                                Esta API gestiona el flujo completo: recolección de actividad comunitaria →
                                análisis con IA → generación de borradores → revisión editorial → publicación.

                                ---

                                ### 🚀 Cómo empezar

                                1. **Crear usuario admin** → `POST /api/v1/auth/register`
                                   ```json
                                   { "email": "admin@talentcircle.com", "password": "Admin123!", "fullName": "Admin", "role": "ADMIN" }
                                   ```

                                2. **Iniciar sesión** → `POST /api/v1/auth/login`
                                   ```json
                                   { "email": "admin@talentcircle.com", "password": "Admin123!" }
                                   ```

                                3. **Copiar el `accessToken`** de la respuesta

                                4. **Hacer clic en 🔒 Authorize** (arriba a la derecha) e ingresar:
                                   ```
                                   Bearer eyJhbGciOiJIUzI1NiJ9...
                                   ```

                                ---

                                ### 📋 Módulos disponibles

                                | Módulo | Estado | Descripción |
                                |--------|--------|-------------|
                                | **Auth** | ✅ | Login, registro, refresh token |
                                | **Admin** | ✅ | Usuarios, fuentes, configuración |
                                | **Collector** | ⏳ | Recolección de actividad comunitaria |
                                | **Drafts** | ⏳ | Panel editorial de borradores |
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TalentCircle Team")
                                .email("dev@talentcircle.com"))
                        .license(new License()
                                .name("Privado - Uso interno")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Desarrollo local")
                ))
                .tags(List.of(
                        new Tag().name("Auth").description("Autenticación JWT — login, registro, refresh, logout"),
                        new Tag().name("Admin › Usuarios").description("Gestión de usuarios del sistema (requiere ADMIN)"),
                        new Tag().name("Admin › Fuentes").description("Gestión de fuentes comunitarias (Discord, Circle, Slack)"),
                        new Tag().name("Admin › Config").description("Configuración del pipeline (prompts LLM, cron, modelo)"),
                        new Tag().name("Admin › Ejecuciones").description("Historial y control de ejecuciones del pipeline"),
                        new Tag().name("Collector").description("Recolección manual de actividad comunitaria"),
                        new Tag().name("Drafts").description("Panel editorial — listar, editar, aprobar y rechazar borradores")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Token JWT obtenido de POST /api/v1/auth/login. " +
                                                "Formato: Bearer {token}")));
    }
}
