package com.talentcircle.config;

import com.talentcircle.common.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
<<<<<<< HEAD
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
=======
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
>>>>>>> d8921cd (integracion del frontend con el backend, estapa de loggin)

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final Environment environment;

<<<<<<< HEAD
    public SecurityConfig(JwtAuthFilter jwtAuthFilter, Environment environment) {
=======
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
>>>>>>> d8921cd (integracion del frontend con el backend, estapa de loggin)
        this.jwtAuthFilter = jwtAuthFilter;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
<<<<<<< HEAD
        boolean isDevProfile = environment.acceptsProfiles(Profiles.of("dev"));

        http
            .csrf(csrf -> {
                if (isDevProfile) {
                    // H2 console requires CSRF disabled for its POST requests
                    csrf.ignoringRequestMatchers("/h2-console/**").disable();
                } else {
                    csrf.disable();
                }
            })
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                if (isDevProfile) {
                    // Allow H2 console only in dev
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                auth
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/webhooks/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                    .anyRequest().authenticated();
            })
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        if (isDevProfile) {
            // H2 console runs inside an iframe — must allow same-origin frames in dev
            http.headers(headers ->
                headers.frameOptions(frame -> frame.sameOrigin()));
        }

        return http.build();
=======
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/webhooks/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
>>>>>>> d8921cd (integracion del frontend con el backend, estapa de loggin)
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos desde la propiedad app.cors.allowed-origins
        // Soporta múltiples orígenes separados por coma: "http://localhost:3000,https://app.example.com"
        List<String> origins = List.of(allowedOrigins.split(","));
        config.setAllowedOrigins(origins.stream().map(String::trim).toList());

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers que el frontend puede enviar (incluye Authorization para JWT)
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Cache-Control"
        ));

        // Headers que el frontend puede leer en la respuesta
        config.setExposedHeaders(List.of("Authorization"));

        // Permite enviar cookies/credenciales (necesario para el flujo de refresh token)
        config.setAllowCredentials(true);

        // Tiempo de caché del preflight en segundos (1 hora)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
