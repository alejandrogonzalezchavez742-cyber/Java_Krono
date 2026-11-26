package com.mycompany.krono.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;

/**
 * Una aparición concreta en el calendario: un evento, el cumpleaños de este año,
 * el corte o el pago de este mes. Varias ocurrencias pueden compartir fecha.
 *
 * @param etiqueta contexto de la junta (Trabajo/Casa/Amigos) o null
 * @param monto    total acumulado del periodo (cortes y pagos) o null
 * @param origen   la entidad de la que sale: Evento, Cumpleanos o Tarjeta
 */
public record Ocurrencia(
        LocalDate fecha,
        CategoriaCalendario categoria,
        String titulo,
        String detalle,
        LocalTime hora,
        boolean prioritaria,
        boolean completada,
        String etiqueta,
        BigDecimal monto,
        Object origen) {

    /** Prioritarias siempre primero; luego pendientes antes que completadas; luego por hora. */
    public static final Comparator<Ocurrencia> ORDEN = Comparator
            .comparing((Ocurrencia o) -> !o.prioritaria())
            .thenComparing(Ocurrencia::completada)
            .thenComparing(Ocurrencia::hora, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(Ocurrencia::categoria)
            .thenComparing(Ocurrencia::titulo, String.CASE_INSENSITIVE_ORDER);

    /** Solo las tareas y juntas se pueden marcar como completadas. */
    public boolean completable() {
        return origen instanceof Evento;
    }
}
