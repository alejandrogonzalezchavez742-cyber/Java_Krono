package com.mycompany.krono.model;

import java.time.LocalDate;
import java.time.MonthDay;

/** Cumpleaños con recurrencia anual. */
public class Cumpleanos {

    private Long id;
    private String nombre;
    private LocalDate fechaNacimiento;
    private boolean anioConocido = true;
    private String notas;

    /**
     * Fecha en que se celebra en el año indicado, o null si aún no había nacido.
     * Quien nació un 29 de febrero lo celebra el 28 en los años no bisiestos.
     */
    public LocalDate ocurrenciaEn(int anio) {
        if (anioConocido && anio < fechaNacimiento.getYear()) {
            return null;
        }
        MonthDay md = MonthDay.from(fechaNacimiento);
        return md.isValidYear(anio) ? md.atYear(anio) : LocalDate.of(anio, 2, 28);
    }

    /** Años que cumple en ese año, o null si no se conoce el año de nacimiento. */
    public Integer edadEn(int anio) {
        return anioConocido ? anio - fechaNacimiento.getYear() : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public boolean isAnioConocido() { return anioConocido; }
    public void setAnioConocido(boolean anioConocido) { this.anioConocido = anioConocido; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
