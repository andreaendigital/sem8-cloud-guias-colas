package com.transportista.guias.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Utilidad para validar y parsear tokens JWT firmados con el algoritmo HS256.
 *
 * <p>Lee la clave secreta desde la propiedad {@code jwt.secret} del archivo
 * {@code application.properties}. Expone métodos para validar el token y
 * extraer los claims {@code sub}, {@code roles} y {@code transportistasPermitidos}.</p>
 *
 * <p>Estructura esperada del payload del JWT:</p>
 * <pre>{@code
 * {
 *   "sub": "usuario@empresa.cl",
 *   "roles": ["ROLE_OPERADOR"],
 *   "transportistasPermitidos": ["T001", "T002"],
 *   "exp": 1700000000
 * }
 * }</pre>
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey secretKey;

    /**
     * Construye la instancia de {@code JwtUtil} inyectando la clave secreta
     * configurada en {@code application.properties} bajo la clave {@code jwt.secret}.
     *
     * @param jwtSecret valor de la propiedad {@code jwt.secret}; debe tener al menos
     *                  32 caracteres para garantizar la fortaleza del secreto HS256.
     */
    public JwtUtil(@Value("${jwt.secret}") String jwtSecret) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Valida un token JWT verificando su firma HS256 y que no haya expirado.
     *
     * <p>Este método nunca lanza excepciones: cualquier error de parseo, firma
     * inválida o token expirado produce un retorno {@code false} con el error
     * registrado en el log a nivel {@code WARN}.</p>
     *
     * @param token el token JWT compacto (formato {@code header.payload.signature})
     * @return {@code true} si el token es válido y no ha expirado; {@code false} en
     *         cualquier otro caso
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("Token JWT inválido o expirado: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Error inesperado al validar token JWT: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae el claim {@code sub} (subject) del token JWT.
     *
     * @param token el token JWT compacto válido
     * @return el valor del claim {@code sub}, o {@code null} si el claim no existe
     * @throws io.jsonwebtoken.JwtException si el token es inválido o ha expirado
     */
    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extrae el claim {@code roles} del token JWT como una lista de strings.
     *
     * <p>Si el claim {@code roles} no está presente en el token, se devuelve
     * una lista vacía inmutable en lugar de lanzar una excepción.</p>
     *
     * @param token el token JWT compacto válido
     * @return lista de roles del usuario (p. ej. {@code ["ROLE_ADMIN", "ROLE_OPERADOR"]}),
     *         nunca {@code null}
     * @throws io.jsonwebtoken.JwtException si el token es inválido o ha expirado
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = parseClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    /**
     * Extrae el claim {@code transportistasPermitidos} del token JWT como una
     * lista de strings con los identificadores de transportista que el usuario
     * tiene permiso de acceder.
     *
     * <p>Si el claim {@code transportistasPermitidos} no está presente en el token,
     * se devuelve una lista vacía inmutable en lugar de lanzar una excepción.</p>
     *
     * @param token el token JWT compacto válido
     * @return lista de {@code transportistaId} permitidos (p. ej. {@code ["T001", "T002"]}),
     *         nunca {@code null}
     * @throws io.jsonwebtoken.JwtException si el token es inválido o ha expirado
     */
    @SuppressWarnings("unchecked")
    public List<String> extractTransportistasPermitidos(String token) {
        Claims claims = parseClaims(token);
        Object transportistas = claims.get("transportistasPermitidos");
        if (transportistas instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    /**
     * Parsea el token JWT y retorna los {@link Claims} verificados.
     *
     * @param token el token JWT compacto
     * @return los claims extraídos y verificados del token
     * @throws io.jsonwebtoken.JwtException si la firma es inválida, el token está
     *                                       malformado o ha expirado
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
