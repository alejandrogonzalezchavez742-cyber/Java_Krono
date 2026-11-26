package com.mycompany.krono.model;

/**
 * Categoría visual de todo lo que aparece en el calendario, con su color.
 * El orden de declaración es el orden de desempate al listar un mismo día.
 */
public enum CategoriaCalendario {
    TAREA("Asuntos importantes", "cat-tarea", "#FF5A5F"),
    JUNTA("Juntas", "cat-junta", "#0284C7"),
    CUMPLEANOS("Cumpleaños", "cat-cumple", "#7C4DFF"),
    CORTE("Cortes de tarjeta", "cat-corte", "#D97706"),
    PAGO("Límite de pago", "cat-pago", "#00C853");

    private final String nombre;
    private final String claseCss;
    private final String colorHex;

    CategoriaCalendario(String nombre, String claseCss, String colorHex) {
        this.nombre = nombre;
        this.claseCss = claseCss;
        this.colorHex = colorHex;
    }

    public String nombre() {
        return nombre;
    }

    /** Clase CSS (cat-tarea, cat-junta…) definida en base.css. */
    public String claseCss() {
        return claseCss;
    }

    public String colorHex() {
        return colorHex;
    }
}
