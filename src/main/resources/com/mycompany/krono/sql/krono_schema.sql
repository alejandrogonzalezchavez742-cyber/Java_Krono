-- =====================================================================
--  KRONO · Esquema de base de datos
--  Motor: PostgreSQL 11 o superior (probado para 17)
--  Base de datos: krono_db
--
--  El script es IDEMPOTENTE: puede ejecutarse varias veces sin perder
--  datos (pgAdmin > Query Tool > F5, o automáticamente desde Krono).
--
--  Decisiones de diseño:
--   * Ninguna tabla tiene restricción UNIQUE sobre la fecha, así que
--     caben MÚLTIPLES eventos el mismo día.
--   * Cumpleaños (anual) y cortes/pagos de tarjeta (mensual) NO se
--     guardan como filas repetidas: se guarda la regla y Krono calcula
--     cada ocurrencia en el calendario.
--   * Las columnas *_aviso / alarma_notificada_en evitan que una alarma
--     se dispare dos veces (por ejemplo, si reinicias Krono).
-- =====================================================================


-- ---------------------------------------------------------------------
-- Metadatos del esquema
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS krono_info (
    clave  VARCHAR(50)  PRIMARY KEY,
    valor  VARCHAR(200) NOT NULL
);

INSERT INTO krono_info (clave, valor)
VALUES ('schema_version', '1')
ON CONFLICT (clave) DO NOTHING;


-- ---------------------------------------------------------------------
-- Función común: mantiene actualizado_en al día en cada UPDATE
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION krono_touch_actualizado()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ---------------------------------------------------------------------
-- EVENTOS: asuntos importantes / tareas (TAREA) y juntas (JUNTA)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS eventos (
    id                    BIGSERIAL    PRIMARY KEY,
    tipo                  VARCHAR(10)  NOT NULL
                          CHECK (tipo IN ('TAREA', 'JUNTA')),
    titulo                VARCHAR(150) NOT NULL
                          CHECK (btrim(titulo) <> ''),
    notas                 TEXT,
    fecha                 DATE         NOT NULL,
    hora                  TIME,                       -- hora de la alarma
    contexto              VARCHAR(10)
                          CHECK (contexto IN ('TRABAJO', 'CASA', 'AMIGOS')),
    prioritaria           BOOLEAN      NOT NULL DEFAULT FALSE,
    completada            BOOLEAN      NOT NULL DEFAULT FALSE,
    completada_en         TIMESTAMP,
    alarma_activa         BOOLEAN      NOT NULL DEFAULT FALSE,
    alarma_notificada_en  TIMESTAMP,
    creado_en             TIMESTAMP    NOT NULL DEFAULT now(),
    actualizado_en        TIMESTAMP    NOT NULL DEFAULT now(),

    -- Una junta exige nombre (titulo), contexto, fecha y hora de alarma
    CONSTRAINT chk_eventos_junta
        CHECK (tipo <> 'JUNTA' OR (contexto IS NOT NULL AND hora IS NOT NULL)),
    -- No puede haber alarma sin hora
    CONSTRAINT chk_eventos_alarma
        CHECK (NOT alarma_activa OR hora IS NOT NULL)
);

-- Consulta del calendario: eventos de un día o de un rango de fechas
CREATE INDEX IF NOT EXISTS idx_eventos_fecha
    ON eventos (fecha);

-- Servicio de alarmas: solo las pendientes de avisar
CREATE INDEX IF NOT EXISTS idx_eventos_alarmas_pendientes
    ON eventos (fecha, hora)
    WHERE alarma_activa AND alarma_notificada_en IS NULL;

DROP TRIGGER IF EXISTS trg_eventos_actualizado ON eventos;
CREATE TRIGGER trg_eventos_actualizado
    BEFORE UPDATE ON eventos
    FOR EACH ROW EXECUTE FUNCTION krono_touch_actualizado();


-- ---------------------------------------------------------------------
-- CUMPLEAÑOS: recurrencia anual, la edad se calcula en Krono
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cumpleanos (
    id                 BIGSERIAL    PRIMARY KEY,
    nombre             VARCHAR(120) NOT NULL
                       CHECK (btrim(nombre) <> ''),
    fecha_nacimiento   DATE         NOT NULL,
    -- FALSE cuando no sabes el año: se muestra el cumpleaños sin edad
    anio_conocido      BOOLEAN      NOT NULL DEFAULT TRUE,
    notas              TEXT,
    ultimo_aviso_anio  SMALLINT,                      -- último año notificado
    creado_en          TIMESTAMP    NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMP    NOT NULL DEFAULT now()
);

DROP TRIGGER IF EXISTS trg_cumpleanos_actualizado ON cumpleanos;
CREATE TRIGGER trg_cumpleanos_actualizado
    BEFORE UPDATE ON cumpleanos
    FOR EACH ROW EXECUTE FUNCTION krono_touch_actualizado();


-- ---------------------------------------------------------------------
-- TARJETAS: recurrencia mensual (día de corte + fecha límite de pago)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tarjetas (
    id                  BIGSERIAL    PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL         -- banco / tarjeta
                        CHECK (btrim(nombre) <> ''),
    -- 29, 30 o 31 en meses más cortos se ajustan al último día del mes
    dia_corte           SMALLINT     NOT NULL
                        CHECK (dia_corte BETWEEN 1 AND 31),
    -- Se usa UNA de las dos: días de gracia tras el corte, o día fijo de pago
    dias_gracia         SMALLINT
                        CHECK (dias_gracia BETWEEN 1 AND 60),
    dia_limite_pago     SMALLINT
                        CHECK (dia_limite_pago BETWEEN 1 AND 31),
    activa              BOOLEAN      NOT NULL DEFAULT TRUE,
    notas               TEXT,
    ultimo_aviso_corte  DATE,                         -- último corte notificado
    ultimo_aviso_pago   DATE,                         -- último pago notificado
    creado_en           TIMESTAMP    NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT chk_tarjetas_pago
        CHECK ((dias_gracia IS NULL) <> (dia_limite_pago IS NULL))
);

DROP TRIGGER IF EXISTS trg_tarjetas_actualizado ON tarjetas;
CREATE TRIGGER trg_tarjetas_actualizado
    BEFORE UPDATE ON tarjetas
    FOR EACH ROW EXECUTE FUNCTION krono_touch_actualizado();


-- ---------------------------------------------------------------------
-- GASTOS por tarjeta. Total del periodo =
--   SUM(monto) WHERE fecha > corte_anterior AND fecha <= corte_actual
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gastos (
    id              BIGSERIAL     PRIMARY KEY,
    tarjeta_id      BIGINT        NOT NULL
                    REFERENCES tarjetas (id) ON DELETE CASCADE,
    concepto        VARCHAR(150)  NOT NULL
                    CHECK (btrim(concepto) <> ''),
    -- Negativo = devolución o bonificación
    monto           NUMERIC(12,2) NOT NULL
                    CHECK (monto <> 0),
    fecha           DATE          NOT NULL,
    notas           TEXT,
    creado_en       TIMESTAMP     NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_gastos_tarjeta_fecha
    ON gastos (tarjeta_id, fecha);

DROP TRIGGER IF EXISTS trg_gastos_actualizado ON gastos;
CREATE TRIGGER trg_gastos_actualizado
    BEFORE UPDATE ON gastos
    FOR EACH ROW EXECUTE FUNCTION krono_touch_actualizado();


-- ---------------------------------------------------------------------
-- CONTACTOS: directorio telefónico en 4 categorías
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS contactos (
    id              BIGSERIAL    PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL
                    CHECK (btrim(nombre) <> ''),
    telefono        VARCHAR(30)  NOT NULL
                    CHECK (btrim(telefono) <> ''),
    categoria       VARCHAR(10)  NOT NULL DEFAULT 'GENERALES'
                    CHECK (categoria IN ('TRABAJO', 'CASA', 'AMIGOS', 'GENERALES')),
    notas           TEXT,
    creado_en       TIMESTAMP    NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMP    NOT NULL DEFAULT now()
);

-- Pestañas por categoría, ordenadas alfabéticamente
CREATE INDEX IF NOT EXISTS idx_contactos_categoria_nombre
    ON contactos (categoria, lower(nombre));

DROP TRIGGER IF EXISTS trg_contactos_actualizado ON contactos;
CREATE TRIGGER trg_contactos_actualizado
    BEFORE UPDATE ON contactos
    FOR EACH ROW EXECUTE FUNCTION krono_touch_actualizado();
