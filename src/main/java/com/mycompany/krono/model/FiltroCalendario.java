package com.mycompany.krono.model;

import java.util.function.Predicate;

/** Opciones del filtro de la barra de controles del calendario. */
public enum FiltroCalendario {
    TODOS("Todas las categorías", o -> true),
    PRIORITARIAS("Solo prioritarias", Ocurrencia::prioritaria),
    JUNTAS("Solo juntas", o -> o.categoria() == CategoriaCalendario.JUNTA),
    TAREAS("Asuntos y tareas", o -> o.categoria() == CategoriaCalendario.TAREA),
    CUMPLEANOS("Cumpleaños", o -> o.categoria() == CategoriaCalendario.CUMPLEANOS),
    TARJETAS("Cortes y pagos", o -> o.categoria() == CategoriaCalendario.CORTE
            || o.categoria() == CategoriaCalendario.PAGO);

    private final String etiqueta;
    private final Predicate<Ocurrencia> condicion;

    FiltroCalendario(String etiqueta, Predicate<Ocurrencia> condicion) {
        this.etiqueta = etiqueta;
        this.condicion = condicion;
    }

    public boolean acepta(Ocurrencia o) {
        return condicion.test(o);
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
