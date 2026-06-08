package com.transportista.guias.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filtro de autenticación JWT que intercepta todas las solicitudes HTTP entrantes,
 * excepto las rutas públicas definidas en el sistema.
 *
 * <p>Este filtro extiende {@link OncePerRequestFilter} para garantizar que la lógica
 * de autenticación se ejecute exactamente una vez por solicitud. Para las rutas
 * protegidas, extrae el token JWT del encabezado {@code Authorization}, lo valida
 * mediante {@link JwtUtil} y, si es válido, carga el {@link org.springframework.security.core.context.SecurityContext}
 * con los roles y el claim {@code transportistasPermitidos} del usuario.</p>
 *
 * <p>Rutas excluidas de la validación JWT (acceso público):</p>
 * <ul>
 *   <li>{@code GET /actuator/health} — health check del servicio</li>
 *   <li>{@code POST /api/v1/guias} — creación de una guía de despacho</li>
 * </ul>
 *
 * <p>Para cualquier otra ruta, si el token está ausente, expirado o tiene firma
 * inválida, el filtro responde con HTTP 401 y un cuerpo JSON con el mensaje de error.</p>
 *
 * @see JwtUtil
 * @see OncePerRequestFilter
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    /** Ruta del health check del actuator (pública, sin autenticación). */
    private static final String ACTUATOR_HEALTH_PATH = "/actuator/health";

    /** Ruta de creación de guías (pública según requisito 5.1). */
    private static final String CREAR_GUIA_PATH = "/api/v1/guias";

    private final JwtUtil jwtUtil;

    /**
     * Construye el filtro inyectando la utilidad JWT para validación y extracción de claims.
     *
     * @param jwtUtil utilidad para validar tokens y extraer claims JWT
     */
    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Lógica principal del filtro: valida el token JWT y carga el {@code SecurityContext}
     * para las rutas protegidas, o delega directamente a la cadena de filtros para las
     * rutas públicas.
     *
     * <p>Flujo de ejecución:</p>
     * <ol>
     *   <li>Verifica si la solicitud corresponde a una ruta pública ({@code GET /actuator/health}
     *       o {@code POST /api/v1/guias}). Si es así, continúa la cadena sin validar token.</li>
     *   <li>Lee el encabezado {@code Authorization}. Si está ausente o no comienza con
     *       {@code Bearer }, responde con HTTP 401.</li>
     *   <li>Extrae el token y lo valida con {@link JwtUtil#validateToken(String)}.
     *       Si es inválido o expirado, responde con HTTP 401.</li>
     *   <li>Extrae {@code sub}, {@code roles} y {@code transportistasPermitidos} del token.</li>
     *   <li>Construye una lista de {@link GrantedAuthority} a partir de los roles.</li>
     *   <li>Crea un {@link UsernamePasswordAuthenticationToken} con el subject como principal,
     *       los roles como authorities, y los {@code transportistasPermitidos} como detalles.</li>
     *   <li>Registra la autenticación en el {@link SecurityContextHolder} y continúa la cadena.</li>
     * </ol>
     *
     * @param request     la solicitud HTTP entrante
     * @param response    la respuesta HTTP saliente
     * @param filterChain la cadena de filtros del contenedor de servlets
     * @throws ServletException si ocurre un error de servlet durante el filtrado
     * @throws IOException      si ocurre un error de E/S durante el filtrado
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Verificar si la ruta es pública y debe omitir la validación JWT
        if (isPublicRoute(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Leer el encabezado Authorization
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            writeUnauthorizedResponse(response);
            return;
        }

        // 3. Extraer y validar el token
        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtUtil.validateToken(token)) {
            writeUnauthorizedResponse(response);
            return;
        }

        // 4. Extraer claims del token
        String subject = jwtUtil.extractSubject(token);
        List<String> roles = jwtUtil.extractRoles(token);
        List<String> transportistasPermitidos = jwtUtil.extractTransportistasPermitidos(token);

        // 5. Construir lista de GrantedAuthority a partir de los roles
        List<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 6. Crear token de autenticación con transportistasPermitidos como detalles
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(subject, null, authorities);
        auth.setDetails(transportistasPermitidos);

        // 7. Registrar en el SecurityContext y continuar la cadena
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }

    /**
     * Determina si la solicitud corresponde a una ruta pública que no requiere
     * validación de token JWT.
     *
     * <p>Rutas públicas:</p>
     * <ul>
     *   <li>{@code GET /actuator/health}</li>
     *   <li>{@code POST /api/v1/guias} (ruta exacta, sin variable de path)</li>
     *   <li>Rutas de la interfaz de Swagger y documentación OpenAPI</li>
     * </ul>
     *
     * @param request la solicitud HTTP a evaluar
     * @return {@code true} si la ruta es pública; {@code false} si requiere autenticación
     */
    private boolean isPublicRoute(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // GET /actuator/health — health check público
        if ("GET".equalsIgnoreCase(method) && ACTUATOR_HEALTH_PATH.equals(path)) {
            return true;
        }

        // POST /api/v1/guias — creación de guía (ruta exacta, sin sufijo adicional)
        if ("POST".equalsIgnoreCase(method) && CREAR_GUIA_PATH.equals(path)) {
            return true;
        }

        // Omitir vaildación JWT para swagger y API Docs
        if(path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            return true;
        }

        return false;
    }

    /**
     * Escribe una respuesta HTTP 401 con cuerpo JSON estándar indicando que el token
     * de autenticación es inválido o está expirado.
     *
     * <p>Formato del cuerpo JSON:</p>
     * <pre>{@code
     * {
     *   "status": 401,
     *   "message": "Token de autenticación inválido o expirado",
     *   "timestamp": "2025-07-15T10:30:00.000Z"
     * }
     * }</pre>
     *
     * @param response la respuesta HTTP donde se escribirá el error
     * @throws IOException si ocurre un error al escribir en el cuerpo de la respuesta
     */
    private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String timestamp = Instant.now().toString();
        String body = String.format(
                "{\"status\":401,\"message\":\"Token de autenticación inválido o expirado\",\"timestamp\":\"%s\"}",
                timestamp
        );

        response.getWriter().write(body);
        response.getWriter().flush();
    }
}
