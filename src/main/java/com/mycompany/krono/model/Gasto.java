package com.mycompany.krono.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Gasto individual asociado a una tarjeta. Un monto negativo es una devolución. */
public class Gasto {

    private Long id;
    private long tarjetaId;
    private String concepto;
    private BigDecimal monto;
    private LocalDate fecha;
    private String notas;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public long getTarjetaId() { return tarjetaId; }
    public void setTarjetaId(long tarjetaId) { this.tarjetaId = tarjetaId; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
