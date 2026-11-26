package com.mycompany.krono.service;

import com.mycompany.krono.dao.CumpleanosDAO;
import com.mycompany.krono.dao.EventoDAO;
import com.mycompany.krono.dao.GastoDAO;
import com.mycompany.krono.dao.TarjetaDAO;
import com.mycompany.krono.model.CategoriaCalendario;
import com.mycompany.krono.model.Cumpleanos;
import com.mycompany.krono.model.Evento;
import com.mycompany.krono.model.Ocurrencia;
import com.mycompany.krono.model.Tarjeta;
import com.mycompany.krono.util.Fechas;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Arma lo que se ve en el calendario. Junta en un solo mapa por día:
 * eventos (una fila cada uno), cumpleaños (anuales) y cortes/pagos de tarjeta (mensuales).
 */
public class CalendarioService {

    /** Los días de gracia llegan hasta 60: un pago visible puede venir de un corte de 2-3 meses antes. */
    private static final int MESES_ATRAS_PARA_PAGOS = 3;

    private final EventoDAO eventoDAO = new EventoDAO();
    private final CumpleanosDAO cumpleanosDAO = new CumpleanosDAO();
    private final TarjetaDAO tarjetaDAO = new TarjetaDAO();
    private final GastoDAO gastoDAO = new GastoDAO();

    /**
     * Ocurrencias entre dos fechas (incluidas), agrupadas por día y ya ordenadas
     * (prioritarias primero). Los días sin nada no aparecen en el mapa.
     */
    public Map<LocalDate, List<Ocurrencia>> cargar(LocalDate desde, LocalDate hasta) throws SQLException {
        Map<LocalDate, List<Ocurrencia>> mapa = new TreeMap<>();

        for (Evento e : eventoDAO.listarEntre(desde, hasta)) {
            agregar(mapa, deEvento(e));
        }

        for (Cumpleanos c : cumpleanosDAO.listarTodos()) {
            for (int anio = desde.getYear(); anio <= hasta.getYear(); anio++) {
                LocalDate dia = c.ocurrenciaEn(anio);
                if (dia != null && dentro(dia, desde, hasta)) {
                    agregar(mapa, deCumpleanos(c, dia));
                }
            }
        }

        for (Tarjeta t : tarjetaDAO.listarActivas()) {
            YearMonth fin = YearMonth.from(hasta);
            for (YearMonth mes = YearMonth.from(desde).minusMonths(MESES_ATRAS_PARA_PAGOS);
                 !mes.isAfter(fin); mes = mes.plusMonths(1)) {
                LocalDate corte = t.corteEn(mes);
                LocalDate pago = t.pagoDeCorte(corte);
                boolean verCorte = dentro(corte, desde, hasta);
                boolean verPago = dentro(pago, desde, hasta);
                if (!verCorte && !verPago) {
                    continue;
                }
                BigDecimal total = gastoDAO.totalPeriodo(t.getId(), t.corteAnterior(corte), corte);
                if (verCorte) {
                    agregar(mapa, new Ocurrencia(corte, CategoriaCalendario.CORTE,
                            "Corte · " + t.getNombre(),
                            "Total acumulado del periodo " + Fechas.diaMes(t.corteAnterior(corte).plusDays(1))
                                    + " al " + Fechas.diaMes(corte),
                            null, false, false, null, total, t));
                }
                if (verPago) {
                    agregar(mapa, new Ocurrencia(pago, CategoriaCalendario.PAGO,
                            "Fecha límite de pago · " + t.getNombre(),
                            "Monto del corte del " + Fechas.diaMes(corte),
                            null, false, false, null, total, t));
                }
            }
        }

        mapa.values().forEach(lista -> lista.sort(Ocurrencia.ORDEN));
        return mapa;
    }

    public void alternarCompletada(Evento e) throws SQLException {
        eventoDAO.marcarCompletada(e.getId(), !e.isCompletada());
    }

    /** Elimina la entidad de origen: el evento, el cumpleaños (todos los años) o la tarjeta con sus gastos. */
    public void eliminar(Ocurrencia o) throws SQLException {
        switch (o.origen()) {
            case Evento e -> eventoDAO.eliminar(e.getId());
            case Cumpleanos c -> cumpleanosDAO.eliminar(c.getId());
            case Tarjeta t -> tarjetaDAO.eliminar(t.getId());
            default -> throw new IllegalArgumentException("Origen desconocido: " + o.origen());
        }
    }

    // ------------------------------------------------------------------ armado

    private static Ocurrencia deEvento(Evento e) {
        return new Ocurrencia(
                e.getFecha(),
                e.esJunta() ? CategoriaCalendario.JUNTA : CategoriaCalendario.TAREA,
                e.getTitulo(),
                e.getNotas(),
                e.getHora(),
                e.isPrioritaria(),
                e.isCompletada(),
                e.getContexto() == null ? null : e.getContexto().etiqueta(),
                null,
                e);
    }

    private static Ocurrencia deCumpleanos(Cumpleanos c, LocalDate dia) {
        Integer edad = c.edadEn(dia.getYear());
        String detalle = edad == null ? c.getNotas()
                : edad == 0 ? "Día de nacimiento"
                : "Cumple " + edad + (edad == 1 ? " año" : " años");
        return new Ocurrencia(dia, CategoriaCalendario.CUMPLEANOS, "Cumpleaños de " + c.getNombre(),
                detalle, null, false, false, null, null, c);
    }

    private static void agregar(Map<LocalDate, List<Ocurrencia>> mapa, Ocurrencia o) {
        mapa.computeIfAbsent(o.fecha(), k -> new ArrayList<>()).add(o);
    }

    private static boolean dentro(LocalDate f, LocalDate desde, LocalDate hasta) {
        return !f.isBefore(desde) && !f.isAfter(hasta);
    }
}
