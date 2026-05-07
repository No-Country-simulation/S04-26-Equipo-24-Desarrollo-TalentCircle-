package com.talentcircle.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OpenAPI / Swagger UI configuration.
 * RNF-13: API REST documentada con OpenAPI 3.
 * Only enabled in dev profile.
 */
@Configuration
@Profile("dev")
public class OpenApiConfig {

    @Bean
    public OpenAPI talentCircleOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("TalentCircle Content Pipeline API")
                        .description("Pipeline de contenido comunitario automatizado para TalentCircle. " +
                                "Gestiona recolección de actividad, análisis IA y generación de borradores.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TalentCircle Team")
                                .email("dev@talentcircle.com")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token obtenido desde POST /api/v1/auth/login")));
    }
}
