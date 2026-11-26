package com.mycompany.krono.model;

/** Contacto del directorio telefónico. */
public class Contacto {

    private Long id;
    private String nombre;
    private String telefono;
    private CategoriaContacto categoria = CategoriaContacto.GENERALES;
    private String notas;

    /** "Ana López" → "AL"; "Plomero" → "P". */
    public String iniciales() {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        String ini = partes[0].substring(0, 1);
        if (partes.length > 1) {
            ini += partes[partes.length - 1].substring(0, 1);
        }
        return ini.toUpperCase();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public CategoriaContacto getCategoria() { return categoria; }
    public void setCategoria(CategoriaContacto categoria) { this.categoria = categoria; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
