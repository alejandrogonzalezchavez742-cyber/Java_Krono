-- =====================================================================
--  KRONO · Datos de ejemplo (OPCIONAL)
--  Sirven para ver el calendario con contenido mientras llegan los
--  formularios de la Fase 4. Las fechas son relativas a HOY.
--
--  Para quitarlos después:
--    TRUNCATE eventos, cumpleanos, gastos, tarjetas, contactos RESTART IDENTITY CASCADE;
-- =====================================================================

-- Hoy: 3 eventos (uno prioritario y una junta)
INSERT INTO eventos (tipo, titulo, notas, fecha, hora, contexto, prioritaria, completada, alarma_activa) VALUES
('TAREA', 'Pagar predial',             'Vence hoy en ventanilla',        CURRENT_DATE,      NULL,    NULL,      TRUE,  FALSE, FALSE),
('JUNTA', 'Junta de equipo',           'Revisión semanal del proyecto',  CURRENT_DATE,      '10:00', 'TRABAJO', FALSE, FALSE, TRUE),
('TAREA', 'Comprar regalo para mamá',  NULL,                             CURRENT_DATE,      NULL,    NULL,      FALSE, FALSE, FALSE),
-- Hace 3 días: una tarea ya completada
('TAREA', 'Cita con el dentista',      NULL,                             CURRENT_DATE - 3,  '16:00', NULL,      FALSE, TRUE,  FALSE),
-- En 2 días: junta con amigos
('JUNTA', 'Cena de cumpleaños',        'Restaurante del centro',         CURRENT_DATE + 2,  '20:30', 'AMIGOS',  FALSE, FALSE, TRUE),
-- En 5 días: 5 eventos + 1 cumpleaños = 6 (se ven 4 puntos y "+2")
('TAREA', 'Renovar licencia',          'Llevar comprobante de domicilio', CURRENT_DATE + 5, NULL,    NULL,      TRUE,  FALSE, FALSE),
('JUNTA', 'Reunión de padres',         'Escuela',                        CURRENT_DATE + 5,  '18:00', 'CASA',    FALSE, FALSE, TRUE),
('TAREA', 'Llamar al plomero',         NULL,                             CURRENT_DATE + 5,  NULL,    NULL,      FALSE, FALSE, FALSE),
('TAREA', 'Enviar reporte mensual',    NULL,                             CURRENT_DATE + 5,  '09:00', NULL,      FALSE, FALSE, TRUE),
('TAREA', 'Revisar seguro del auto',   NULL,                             CURRENT_DATE + 5,  NULL,    NULL,      FALSE, FALSE, FALSE);

-- Cumpleaños (recurrencia anual). Lucía sin año conocido: no muestra edad.
INSERT INTO cumpleanos (nombre, fecha_nacimiento, anio_conocido) VALUES
('Ana López',   (CURRENT_DATE     - INTERVAL '30 years')::date, TRUE),
('Carlos Ruiz', (CURRENT_DATE + 5 - INTERVAL '41 years')::date, TRUE),
('Lucía',       (CURRENT_DATE + 9 - INTERVAL '25 years')::date, FALSE);

-- Tarjetas (recurrencia mensual): una con días de gracia y otra con día fijo de pago
INSERT INTO tarjetas (nombre, dia_corte, dias_gracia)
VALUES ('BBVA Azul', EXTRACT(DAY FROM CURRENT_DATE + 3)::smallint, 20);

INSERT INTO tarjetas (nombre, dia_corte, dia_limite_pago)
VALUES ('Banamex Oro', EXTRACT(DAY FROM CURRENT_DATE + 8)::smallint, EXTRACT(DAY FROM CURRENT_DATE + 12)::smallint);

-- Gastos del periodo actual: se suman en el corte y en la fecha de pago
INSERT INTO gastos (tarjeta_id, concepto, monto, fecha)
SELECT t.id, g.concepto, g.monto, CURRENT_DATE + g.dias
FROM tarjetas t
CROSS JOIN (VALUES ('Supermercado', 1250.40, -10),
                   ('Gasolina',      800.00,  -4),
                   ('Streaming',     219.00,  -1)) AS g(concepto, monto, dias)
WHERE t.nombre = 'BBVA Azul';

INSERT INTO gastos (tarjeta_id, concepto, monto, fecha)
SELECT t.id, 'Boletos de avión', 4380.00, CURRENT_DATE - 6
FROM tarjetas t WHERE t.nombre = 'Banamex Oro';

-- Directorio: las 4 categorías
INSERT INTO contactos (nombre, telefono, categoria, notas) VALUES
('Ana López',          '55 1234 5678',    'AMIGOS',    'Cumple el mismo día que su hermana'),
('Carlos Ruiz',        '55 8765 4321',    'TRABAJO',   'Gerente de proyecto · ext. 204'),
('Oficina central',    '(55) 5000 1200',  'TRABAJO',   NULL),
('Mamá',               '33 3615 0000',    'CASA',      NULL),
('Plomero Don Jorge',  '55 4455 6677',    'GENERALES', 'Cobra $350 la visita'),
('Farmacia 24 h',      '800 911 2000',    'GENERALES', NULL),
('Lucía',              '81 2233 4455',    'AMIGOS',    NULL);

-- Para quitar TODO lo de ejemplo (incluidos los contactos):
--   TRUNCATE eventos, cumpleanos, gastos, tarjetas, contactos RESTART IDENTITY CASCADE;
