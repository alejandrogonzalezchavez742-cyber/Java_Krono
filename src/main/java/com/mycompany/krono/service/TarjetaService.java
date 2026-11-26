package com.mycompany.krono.service;

import com.mycompany.krono.dao.GastoDAO;
import com.mycompany.krono.dao.TarjetaDAO;
import com.mycompany.krono.model.Gasto;
import com.mycompany.krono.model.Tarjeta;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Periodos de corte, totales y gastos de las tarjetas. */
public class TarjetaService {

    /**
     * Un periodo de facturación: del día siguiente al corte anterior ({@code inicio}) hasta el
     * corte ({@code corte}), ambos incluidos, y su fecha límite de pago.
     */
    public record Periodo(LocalDate inicio, LocalDate corte, LocalDate pago) {

        public boolean contiene(LocalDate f) {
            return !f.isBefore(inicio) && !f.isAfter(corte);
        }
    }

    /** Lo que muestra cada tarjeta en la lista: su periodo actual y lo acumulado. */
    public record ResumenTarjeta(Tarjeta tarjeta, Periodo periodo, BigDecimal total) {
    }

    private final TarjetaDAO tarjetaDAO = new TarjetaDAO();
    private final GastoDAO gastoDAO = new GastoDAO();

    // ------------------------------------------------------------ periodos

    public static Periodo periodoDeCorte(Tarjeta t, LocalDate corte) {
        return new Periodo(t.corteAnterior(corte).plusDays(1), corte, t.pagoDeCorte(corte));
    }

    /** Periodo al que pertenece un gasto hecho en {@code fecha}. */
    public static Periodo periodoQueContiene(Tarjeta t, LocalDate fecha) {
        LocalDate corte = t.corteEn(YearMonth.from(fecha));
        if (fecha.isAfter(corte)) {
            corte = t.corteEn(YearMonth.from(fecha).plusMonths(1));
        }
        return periodoDeCorte(t, corte);
    }

    public static Periodo desplazar(Tarjeta t, Periodo p, int meses) {
        return periodoDeCorte(t, t.corteEn(YearMonth.from(p.corte()).plusMonths(meses)));
    }

    // ---------------------------------------------------------------- datos

    public List<ResumenTarjeta> resumenes(LocalDate hoy) throws SQLException {
        List<ResumenTarjeta> lista = new ArrayList<>();
        for (Tarjeta t : tarjetaDAO.listarTodas()) {
            Periodo p = periodoQueContiene(t, hoy);
            lista.add(new ResumenTarjeta(t, p, total(t, p)));
        }
        return lista;
    }

    public List<Tarjeta> tarjetasActivas() throws SQLException {
        return tarjetaDAO.listarActivas();
    }

    public BigDecimal total(Tarjeta t, Periodo p) throws SQLException {
        return gastoDAO.totalPeriodo(t.getId(), p.inicio().minusDays(1), p.corte());
    }

    public List<Gasto> gastos(Tarjeta t, Periodo p) throws SQLException {
        return gastoDAO.listarPeriodo(t.getId(), p.inicio().minusDays(1), p.corte());
    }
}
