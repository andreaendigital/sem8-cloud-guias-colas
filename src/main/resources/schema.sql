-- =============================================================================
-- Schema de inicialización: Sistema de Gestión de Guías de Despacho
-- =============================================================================
-- Tabla principal que almacena todas las guías de despacho.
-- El campo guia_id es un UUID v4 generado por la capa de aplicación (JPA).
-- La eliminación es lógica: el campo "eliminado" se marca TRUE y el estado
-- pasa a ELIMINADA sin borrar físicamente el registro.
--
-- Índice compuesto en (transportista_id, fecha_envio) para optimizar la
-- consulta principal de listado filtrado por transportista y período mensual
-- (Requisito 8.1).
-- =============================================================================

CREATE TABLE IF NOT EXISTS guias (
    guia_id           CHAR(36)       NOT NULL PRIMARY KEY,
    transportista_id  VARCHAR(50)    NOT NULL,
    fecha_envio       DATE           NOT NULL,
    destinatario      VARCHAR(255)   NOT NULL,
    direccion_destino VARCHAR(500)   NOT NULL,
    peso_kg           DECIMAL(10,3),
    descripcion_carga VARCHAR(1000),
    observaciones     VARCHAR(2000),
    estado            VARCHAR(20)    NOT NULL DEFAULT 'BORRADOR',
    url_s3            VARCHAR(1000),
    fecha_creacion    TIMESTAMP      NOT NULL,
    fecha_actualizacion TIMESTAMP,
    eliminado         BOOLEAN        NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_transportista_fecha ON guias (transportista_id, fecha_envio);
