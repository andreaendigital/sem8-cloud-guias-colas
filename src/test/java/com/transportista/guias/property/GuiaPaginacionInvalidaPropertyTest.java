package com.transportista.guias.property;

import com.transportista.guias.service.GuiaServiceImpl;
import com.transportista.guias.repository.GuiaRepository;
import com.transportista.guias.service.PdfGeneratorService;
import com.transportista.guias.service.S3StorageService;
import com.transportista.guias.security.JwtUtil;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de propiedad — Property 7: Parámetros de paginación fuera de rango
 * siempre producen HTTP 400 (representado como {@link IllegalArgumentException}
 * en la capa de servicio).
 *
 * <p>Para cualquier combinación donde {@code size > 100}, {@code page < 0} o
 * {@code size < 1}, el método {@code consultarGuias} debe lanzar
 * {@link IllegalArgumentException} con un mensaje descriptivo.
 * El {@code GlobalExceptionHandler} lo convierte a HTTP 400.</p>
 *
 * <p><b>Valida:</b> Requirements 8.8, 8.9</p>
 */
public class GuiaPaginacionInvalidaPropertyTest {

    /** Stub de dependencias — no se invocan en este path. */
    private final GuiaRepository guiaRepository = Mockito.mock(GuiaRepository.class);
    private final PdfGeneratorService pdfGeneratorService = Mockito.mock(PdfGeneratorService.class);
    private final S3StorageService s3StorageService = Mockito.mock(S3StorageService.class);
    private final JwtUtil jwtUtil = Mockito.mock(JwtUtil.class);

    private final GuiaServiceImpl service = new GuiaServiceImpl(
            guiaRepository, pdfGeneratorService, s3StorageService, jwtUtil);

    // -------------------------------------------------------------------------
    // size > 100 → IllegalArgumentException
    // -------------------------------------------------------------------------

    /**
     * Property 7a: size mayor a 100 siempre lanza {@link IllegalArgumentException}.
     *
     * @param size valor de tamaño de página fuera de rango superior (101–1000)
     */
    @Property(tries = 200)
    void sizeMayorA100LanzaExcepcion(
            @ForAll @IntRange(min = 101, max = 1000) int size
    ) {
        assertThatThrownBy(() ->
                service.consultarGuias("T001", "202507", 0, size))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

    // -------------------------------------------------------------------------
    // page < 0 → IllegalArgumentException
    // -------------------------------------------------------------------------

    /**
     * Property 7b: page negativo siempre lanza {@link IllegalArgumentException}.
     *
     * @param page valor de página fuera de rango inferior (-1000 a -1)
     */
    @Property(tries = 200)
    void pageNegativoLanzaExcepcion(
            @ForAll @IntRange(min = -1000, max = -1) int page
    ) {
        assertThatThrownBy(() ->
                service.consultarGuias("T001", "202507", page, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paginación");
    }

    // -------------------------------------------------------------------------
    // size < 1 → IllegalArgumentException
    // -------------------------------------------------------------------------

    /**
     * Property 7c: size menor a 1 siempre lanza {@link IllegalArgumentException}.
     *
     * @param size valor de tamaño de página fuera de rango inferior (-100 a 0)
     */
    @Property(tries = 200)
    void sizeMenorA1LanzaExcepcion(
            @ForAll @IntRange(min = -100, max = 0) int size
    ) {
        assertThatThrownBy(() ->
                service.consultarGuias("T001", "202507", 0, size))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
