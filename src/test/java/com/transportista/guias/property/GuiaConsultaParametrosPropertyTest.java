package com.transportista.guias.property;

import com.transportista.guias.repository.GuiaRepository;
import com.transportista.guias.service.GuiaServiceImpl;
import com.transportista.guias.service.PdfGeneratorService;
import com.transportista.guias.service.S3StorageService;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de propiedad — Property 8: Validación de parámetros obligatorios de consulta.
 *
 * <p>Para cualquier solicitud {@code GET /api/v1/guias} que:</p>
 * <ul>
 *   <li>Omita {@code transportistaId} (nulo o en blanco), o</li>
 *   <li>Tenga un valor de {@code fecha} que no cumpla el formato {@code YYYYMM}
 *       con mes en rango 01–12 (strings vacíos, letras, meses fuera de rango)</li>
 * </ul>
 * <p>el sistema debe lanzar {@link IllegalArgumentException}, que el
 * {@code GlobalExceptionHandler} convierte a HTTP 400.</p>
 *
 * <p><b>Valida:</b> Requirements 8.4, 8.5</p>
 */
public class GuiaConsultaParametrosPropertyTest {

    /** Stub de dependencias — no se invocan en validación de parámetros. */
    private final GuiaRepository guiaRepository = Mockito.mock(GuiaRepository.class);
    private final PdfGeneratorService pdfGeneratorService = Mockito.mock(PdfGeneratorService.class);
    private final S3StorageService s3StorageService = Mockito.mock(S3StorageService.class);

    private final GuiaServiceImpl service = new GuiaServiceImpl(
            guiaRepository, pdfGeneratorService, s3StorageService);

    // -------------------------------------------------------------------------
    // transportistaId ausente o en blanco → HTTP 400
    // -------------------------------------------------------------------------

    /**
     * Property 8a: transportistaId nulo lanza {@link IllegalArgumentException}.
     */
    @Property(tries = 50)
    void transportistaIdNuloLanzaExcepcion() {
        assertThatThrownBy(() ->
                service.consultarGuias(null, "202507", 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transportistaId");
    }

    /**
     * Property 8b: transportistaId en blanco (solo espacios) lanza
     * {@link IllegalArgumentException}.
     *
     * @param espacios string con solo espacios en blanco
     */
    @Property(tries = 100)
    void transportistaIdEnBlancoLanzaExcepcion(
            @ForAll("soloEspacios") String espacios
    ) {
        assertThatThrownBy(() ->
                service.consultarGuias(espacios, "202507", 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transportistaId");
    }

    // -------------------------------------------------------------------------
    // fecha con formato inválido → HTTP 400
    // -------------------------------------------------------------------------

    /**
     * Property 8c: fecha con menos o más de 6 dígitos lanza
     * {@link IllegalArgumentException}.
     *
     * @param fecha string numérico de longitud distinta a 6
     */
    @Property(tries = 200)
    void fechaConLongitudInvalidaLanzaExcepcion(
            @ForAll("digitosNoSeisCaracteres") String fecha
    ) {
        assertThatThrownBy(() ->
                service.consultarGuias("T001", fecha, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha");
    }

    /**
     * Property 8d: fecha con mes fuera del rango 01–12 lanza
     * {@link IllegalArgumentException}.
     *
     * @param mesInvalido mes representado como string de 2 dígitos fuera de 01–12
     */
    @Property(tries = 200)
    void fechaConMesInvalidoLanzaExcepcion(
            @ForAll("mesInvalido") String mesInvalido
    ) {
        String fecha = "2025" + mesInvalido;
        assertThatThrownBy(() ->
                service.consultarGuias("T001", fecha, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha");
    }

    /**
     * Property 8e: fecha con letras (no numérica) lanza
     * {@link IllegalArgumentException}.
     *
     * @param fechaConLetras string que contiene al menos una letra
     */
    @Property(tries = 200)
    void fechaConLetrasLanzaExcepcion(
            @ForAll("fechaConLetras") String fechaConLetras
    ) {
        assertThatThrownBy(() ->
                service.consultarGuias("T001", fechaConLetras, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha");
    }

    // -------------------------------------------------------------------------
    // Providers (arbitrarios jqwik)
    // -------------------------------------------------------------------------

    /** Genera strings compuestos únicamente por espacios (1–5 espacios). */
    @Provide
    Arbitrary<String> soloEspacios() {
        return Arbitraries.integers().between(1, 5)
                .map(n -> " ".repeat(n));
    }

    /** Genera strings numéricos de longitud distinta a 6 (0–5 y 7–12 caracteres). */
    @Provide
    Arbitrary<String> digitosNoSeisCaracteres() {
        Arbitrary<String> cortos = Arbitraries.integers().between(0, 5)
                .flatMap(len -> len == 0
                        ? Arbitraries.just("")
                        : Arbitraries.strings().withCharRange('0', '9').ofLength(len));
        Arbitrary<String> largos = Arbitraries.integers().between(7, 12)
                .flatMap(len -> Arbitraries.strings().withCharRange('0', '9').ofLength(len));
        return Arbitraries.oneOf(cortos, largos);
    }

    /** Genera strings de 2 dígitos con mes fuera del rango válido 01–12 (00 o 13–99). */
    @Provide
    Arbitrary<String> mesInvalido() {
        Arbitrary<String> cero = Arbitraries.just("00");
        Arbitrary<String> mayores = Arbitraries.integers().between(13, 99)
                .map(m -> String.format("%02d", m));
        return Arbitraries.oneOf(cero, mayores);
    }

    /** Genera strings de longitud 6 que contienen al menos una letra. */
    @Provide
    Arbitrary<String> fechaConLetras() {
        return Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
                .ofLength(6)
                .filter(s -> s.chars().anyMatch(Character::isLetter));
    }
}
