package com.mycompany.krono.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.concurrent.Task;

/**
 * Ejecuta el acceso a la base de datos fuera del hilo de JavaFX para que la ventana nunca
 * se congele. Un único hilo: las operaciones se aplican en el orden en que se piden.
 */
public final class Tareas {

    private static final ExecutorService BD = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "krono-bd");
        t.setDaemon(true);
        return t;
    });

    private Tareas() {
    }

    /** {@code alTerminar} y {@code alFallar} se llaman en el hilo de JavaFX. */
    public static <T> void ejecutar(Callable<T> trabajo, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.call();
            }
        };
        tarea.setOnSucceeded(e -> alTerminar.accept(tarea.getValue()));
        tarea.setOnFailed(e -> alFallar.accept(tarea.getException()));
        BD.execute(tarea);
    }
}
