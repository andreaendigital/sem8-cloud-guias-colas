package com.transportista.guias.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad basada en OAuth2 / Azure AD B2C.
 *
 * <p>El microservicio actúa como <b>Resource Server</b>: valida tokens JWT
 * emitidos por Azure AD B2C usando el JWK Set URI configurado en
 * {@code application.properties}.</p>
 *
 * <p>{@code @EnableMethodSecurity} habilita el control de acceso por método
 * mediante {@code @PreAuthorize} en los controladores.</p>
 *
 * <p>Configuración en application.properties:
 * <pre>
 * spring.security.oauth2.resourceserver.jwt.issuer-uri=https://duocrosero.b2clogin.com/...
 * spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://duocrosero.b2clogin.com/...
 * </pre>
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Habilita @PreAuthorize en los controladores
public class SecurityConfig {

    /**
     * Cadena de filtros de seguridad OAuth2 Resource Server.
     *
     * <ul>
     *   <li>CSRF deshabilitado — API stateless.</li>
     *   <li>Swagger UI y health check son públicos.</li>
     *   <li>Todos los demás endpoints requieren token JWT válido de Azure AD B2C.</li>
     *   <li>Los permisos por rol se controlan con {@code @PreAuthorize} en cada endpoint.</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // API stateless — sin CSRF
            .csrf(csrf -> csrf.disable())

            // Reglas de autorización
            .authorizeHttpRequests(auth -> auth
                // Swagger y documentación OpenAPI — públicos
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()
                // Health check — público
                .requestMatchers("/actuator/health").permitAll()
                // Todo lo demás requiere token válido de Azure AD B2C
                .anyRequest().authenticated()
            )

            // Resource Server: valida JWT contra el JWK Set URI de Azure AD B2C
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})  // Spring auto-configura issuer-uri y jwk-set-uri
            );

        return http.build();
    }
}
