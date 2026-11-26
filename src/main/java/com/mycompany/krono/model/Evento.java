package com.mycompany.krono.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Fila de la tabla eventos: asunto/tarea o junta. */
public class Evento {

    private Long id;
    private TipoEvento tipo = TipoEvento.TAREA;
    private String titulo;
    private String notas;
    private LocalDate fecha;
    private LocalTime hora;
    private ContextoJunta contexto;
    private boolean prioritaria;
    private boolean completada;
    private boolean alarmaActiva;

    public boolean esJunta() {
        return tipo == TipoEvento.JUNTA;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoEvento getTipo() { return tipo; }
    public void setTipo(TipoEvento tipo) { this.tipo = tipo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }

    public ContextoJunta getContexto() { return contexto; }
    public void setContexto(ContextoJunta contexto) { this.contexto = contexto; }

    public boolean isPrioritaria() { return prioritaria; }
    public void setPrioritaria(boolean prioritaria) { this.prioritaria = prioritaria; }

    public boolean isCompletada() { return completada; }
    public void setCompletada(boolean completada) { this.completada = completada; }

    public boolean isAlarmaActiva() { return alarmaActiva; }
    public void setAlarmaActiva(boolean alarmaActiva) { this.alarmaActiva = alarmaActiva; }
}
