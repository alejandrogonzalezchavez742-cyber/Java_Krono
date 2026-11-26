package com.mycompany.krono.model;

/** Las 4 categorías del directorio. */
public enum CategoriaContacto {
    TRABAJO("Trabajo", "cc-trabajo"),
    CASA("Casa", "cc-casa"),
    AMIGOS("Amigos", "cc-amigos"),
    GENERALES("Generales", "cc-generales");

    private final String etiqueta;
    private final String claseCss;

    CategoriaContacto(String etiqueta, String claseCss) {
        this.etiqueta = etiqueta;
        this.claseCss = claseCss;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String claseCss() {
        return claseCss;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
