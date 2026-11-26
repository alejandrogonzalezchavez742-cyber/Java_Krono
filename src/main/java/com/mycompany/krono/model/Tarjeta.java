package com.mycompany.krono.model;

import com.mycompany.krono.util.Fechas;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Tarjeta de crédito con recurrencia mensual: un corte y una fecha límite de pago por mes.
 * La fecha de pago se define con días de gracia o con un día fijo del mes, nunca con ambos.
 */
public class Tarjeta {

    private Long id;
    private String nombre;
    private int diaCorte;
    private Integer diasGracia;
    private Integer diaLimitePago;
    private boolean activa = true;
    private String notas;

    /** Fecha de corte del mes. Un corte día 31 cae el 30 en abril y el 28/29 en febrero. */
    public LocalDate corteEn(YearMonth mes) {
        return Fechas.ajustarDia(mes, diaCorte);
    }

    public LocalDate corteAnterior(LocalDate corte) {
        return corteEn(YearMonth.from(corte).minusMonths(1));
    }

    /** Fecha límite para pagar lo acumulado en el corte indicado. */
    public LocalDate pagoDeCorte(LocalDate corte) {
        if (diasGracia != null) {
            return corte.plusDays(diasGracia);
        }
        YearMonth mes = YearMonth.from(corte);
        // Día de pago menor o igual al de corte: el pago cae en el mes siguiente
        if (diaLimitePago <= diaCorte) {
            mes = mes.plusMonths(1);
        }
        return Fechas.ajustarDia(mes, diaLimitePago);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getDiaCorte() { return diaCorte; }
    public void setDiaCorte(int diaCorte) { this.diaCorte = diaCorte; }

    public Integer getDiasGracia() { return diasGracia; }
    public void setDiasGracia(Integer diasGracia) { this.diasGracia = diasGracia; }

    public Integer getDiaLimitePago() { return diaLimitePago; }
    public void setDiaLimitePago(Integer diaLimitePago) { this.diaLimitePago = diaLimitePago; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
