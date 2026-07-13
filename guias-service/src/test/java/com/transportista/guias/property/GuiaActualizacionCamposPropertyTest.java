package com.transportista.guias.property;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.transportista.guias.dto.ActualizarGuiaRequestDTO;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de propiedad — Property 5: Campos no permitidos en actualización
 * siempre producen un error de deserialización (HTTP 400 en la capa web).
 *
 * <p>Verifica que {@link ActualizarGuiaRequestDTO}, anotado con
 * {@code @JsonIgnoreProperties(ignoreUnknown = false)}, lance
 * {@link UnrecognizedPropertyException} al deserializar JSON con cualquier
 * campo fuera del conjunto permitido. El {@code GlobalExceptionHandler}
 * convierte esa excepción a HTTP 400.</p>
 *
 * <p><b>Valida:</b> Requirement 6.2</p>
 */
public class GuiaActualizacionCamposPropertyTest {

    /** Campos cuya presencia debe provocar un error de deserialización. */
    private static final String[] CAMPOS_NO_PERMITIDOS = {
            "transportistaId", "fechaEnvio", "guiaId", "estado",
            "urlS3", "fechaCreacion", "fechaActualizacion", "eliminado"
    };

    /**
     * Jackson configurado exactamente como lo usa Spring Boot:
     * FAIL_ON_UNKNOWN_PROPERTIES activo para respetar @JsonIgnoreProperties(ignoreUnknown=false).
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * Property 5: cualquier JSON con campo no permitido lanza
     * {@link UnrecognizedPropertyException} al deserializar hacia
     * {@link ActualizarGuiaRequestDTO}.
     *
     * @param valorCampoNoPermitido valor aleatorio para el campo no permitido
     */
    @Property(tries = 200)
    void campoNoPermitidoProduceErrorDeserializacion(
            @ForAll @StringLength(min = 1, max = 20) String valorCampoNoPermitido
    ) {
        String campoNoPermitido = CAMPOS_NO_PERMITIDOS[
                Math.abs(valorCampoNoPermitido.hashCode()) % CAMPOS_NO_PERMITIDOS.length];

        // JSON con campo válido + campo no permitido
        String json = String.format(
                "{\"destinatario\":\"Test\",\"%s\":\"%s\"}",
                campoNoPermitido,
                valorCampoNoPermitido.replace("\"", "'"));

        assertThatThrownBy(() ->
                objectMapper.readValue(json, ActualizarGuiaRequestDTO.class))
                .isInstanceOf(UnrecognizedPropertyException.class)
                .hasMessageContaining(campoNoPermitido);
    }
}
