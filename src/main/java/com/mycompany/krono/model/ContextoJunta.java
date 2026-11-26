package com.mycompany.krono.model;

/** Contexto o grupo de una junta. Se muestra como etiqueta: [Trabajo] Junta de equipo. */
public enum ContextoJunta {
    TRABAJO("Trabajo"),
    CASA("Casa"),
    AMIGOS("Amigos");

    private final String etiqueta;

    ContextoJunta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
