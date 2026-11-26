package com.mycompany.krono.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Aviso de "los datos cambiaron": cuando se guarda algo en cualquier vista (un gasto en
 * Tarjetas, una junta en el Calendario…), las demás vistas se vuelven a cargar.
 * Se usa siempre desde el hilo de JavaFX.
 */
public final class Cambios {

    private static final List<Runnable> OYENTES = new CopyOnWriteArrayList<>();

    private Cambios() {
    }

    public static void escuchar(Runnable oyente) {
        OYENTES.add(oyente);
    }

    public static void notificar() {
        OYENTES.forEach(Runnable::run);
    }
}
