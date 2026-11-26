package com.mycompany.krono;

/**
 * Clase principal del .jar ejecutable (maven-shade-plugin).
 *
 * Al ejecutar un fat-jar en el classpath, si la clase Main extiende Application la JVM aborta con
 * "faltan los componentes de JavaFX runtime". Este lanzador intermedio, que no extiende
 * Application, evita ese problema.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
