package com.transportista.guias.config;

import com.transportista.guias.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad — MODO DESARROLLO (seguridad JWT desactivada).
 *
 * TODO: Reactivar para Experiencia 2:
 *   - Descomentar inyección de jwtAuthFilter
 *   - Descomentar .addFilterBefore(...)
 *   - Restaurar reglas de autorización por rol
 *
 * @see JwtAuthFilter  (preservado, listo para reactivar)
 */
@Configuration
@EnableWebSecurity
public class JwtSecurityConfig {

    // TODO: Reactivar para Experiencia 2
    // private final JwtAuthFilter jwtAuthFilter;
    //
    // public JwtSecurityConfig(JwtAuthFilter jwtAuthFilter) {
    //     this.jwtAuthFilter = jwtAuthFilter;
    // }

    /**
     * Cadena de filtros con seguridad completamente abierta para desarrollo local.
     * Todo el tráfico es permitido sin autenticación.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF deshabilitado (API stateless)
            .csrf(csrf -> csrf.disable())

            // Permitir absolutamente todo — sin autenticación requerida
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

            // TODO: Reactivar para Experiencia 2
            // .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
