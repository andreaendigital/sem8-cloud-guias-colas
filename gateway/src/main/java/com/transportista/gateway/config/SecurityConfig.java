package com.transportista.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuración de seguridad reactiva del API Gateway.
 *
 * <p>Valida tokens JWT de Azure AD B2C en cada request entrante antes de
 * hacer proxy al microservicio destino. Esto constituye la primera capa de
 * seguridad; cada microservicio tiene su propia validación como segunda capa.</p>
 *
 * <p>Rutas públicas (sin JWT requerido):</p>
 * <ul>
 *   <li>{@code /actuator/**} — health checks del gateway</li>
 * </ul>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges
                // Actuator del gateway es público (para health checks de Docker)
                .pathMatchers("/actuator/**").permitAll()
                // Todo lo demás requiere JWT válido de Azure AD B2C
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            );
        return http.build();
    }
}
