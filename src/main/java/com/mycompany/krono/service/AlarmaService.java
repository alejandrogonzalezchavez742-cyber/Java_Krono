package com.mycompany.krono.service;

import com.mycompany.krono.dao.CumpleanosDAO;
import com.mycompany.krono.dao.EventoDAO;
import com.mycompany.krono.dao.GastoDAO;
import com.mycompany.krono.dao.TarjetaDAO;
import com.mycompany.krono.model.Cumpleanos;
import com.mycompany.krono.model.Evento;
import com.mycompany.krono.model.Tarjeta;
import com.mycompany.krono.service.NotificacionService.Aviso;
import com.mycompany.krono.util.Fechas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servicio en segundo plano que cada {@value #INTERVALO_SEGUNDOS} s revisa:
 *  - alarmas de juntas y tareas cuya hora ya llegó (todas, aunque compartan fecha y hora);
 *  - a partir de las 9:00, cumpleaños del día, cortes de tarjeta (con el total acumulado)
 *    y fechas límite de pago.
 * Cada aviso se marca en la base de datos al tomarlo, así no se repite aunque reinicies Krono.
 */
public class AlarmaService {

    private static final int INTERVALO_SEGUNDOS = 30;
    /** Hora a partir de la cual se avisan cumpleaños, cortes y pagos del día. */
    private static final LocalTime HORA_AVISOS_DIARIOS = LocalTime.of(9, 0);
    /** Alarmas vencidas de hasta este número de días atrás se avisan al abrir Krono. */
    private static final int DIAS_ALARMAS_ATRASADAS = 1;

    private final EventoDAO eventoDAO = new EventoDAO();
    private final CumpleanosDAO cumpleanosDAO = new CumpleanosDAO();
    private final TarjetaDAO tarjetaDAO = new TarjetaDAO();
    private final GastoDAO gastoDAO = new GastoDAO();
    private final NotificacionService notificaciones;

    private ScheduledExecutorService ejecutor;

    public AlarmaService(NotificacionService notificaciones) {
        this.notificaciones = notificaciones;
    }

    public synchronized void iniciar() {
        if (ejecutor != null) {
            return;
        }
        ejecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "krono-alarmas");
            t.setDaemon(true);
            return t;
        });
        ejecutor.scheduleWithFixedDelay(this::revisarSinFallar, 3, INTERVALO_SEGUNDOS, TimeUnit.SECONDS);
    }

    public synchronized void detener() {
        if (ejecutor != null) {
            ejecutor.shutdownNow();
            ejecutor = null;
        }
    }

    /** Una excepción no controlada cancelaría la programación: se registra y se sigue. */
    private void revisarSinFallar() {
        try {
            notificaciones.notificar(revisar(LocalDateTime.now()));
        } catch (Exception e) {
            System.err.println("[Krono] Revisión de alarmas fallida (se reintenta en "
                    + INTERVALO_SEGUNDOS + " s): " + e.getMessage());
        }
    }

    /** Devuelve los avisos que tocan ahora y los marca como avisados. */
    List<Aviso> revisar(LocalDateTime ahora) throws Exception {
        LocalDate hoy = ahora.toLocalDate();
        List<Aviso> avisos = new ArrayList<>();

        for (Evento e : eventoDAO.tomarAlarmasVencidas(ahora, hoy.minusDays(DIAS_ALARMAS_ATRASADAS))) {
            avisos.add(avisoDeEvento(e));
        }

        if (!ahora.toLocalTime().isBefore(HORA_AVISOS_DIARIOS)) {
            avisosDeCumpleanos(hoy, avisos);
            avisosDeTarjetas(hoy, avisos);
        }
        return avisos;
    }

    // --------------------------------------------------------------- eventos

    static Aviso avisoDeEvento(Evento e) {
        String detalle = e.getNotas() == null || e.getNotas().isBlank() ? "" : " · " + e.getNotas();
        String cuando = e.getFecha().equals(LocalDate.now()) ? "" : " (" + Fechas.diaMes(e.getFecha()) + ")";
        if (e.esJunta()) {
            String etiqueta = e.getContexto() == null ? "" : "[" + e.getContexto().etiqueta() + "] ";
            return new Aviso(etiqueta + e.getTitulo(),
                    "Junta a las " + Fechas.hora(e.getHora()) + cuando + detalle);
        }
        return new Aviso((e.isPrioritaria() ? "★ Prioritaria: " : "Recordatorio: ") + e.getTitulo(),
                Fechas.hora(e.getHora()) + cuando + detalle);
    }

    // ------------------------------------------------------------ cumpleaños

    private void avisosDeCumpleanos(LocalDate hoy, List<Aviso> avisos) throws Exception {
        for (Cumpleanos c : cumpleanosDAO.listarTodos()) {
            if (hoy.equals(c.ocurrenciaEn(hoy.getYear())) && cumpleanosDAO.reclamarAviso(c.getId(), hoy.getYear())) {
                Integer edad = c.edadEn(hoy.getYear());
                avisos.add(new Aviso("Hoy es el cumpleaños de " + c.getNombre(),
                        edad == null ? "¡No olvides felicitarle!"
                                : "Cumple " + edad + (edad == 1 ? " año" : " años") + ". ¡No olvides felicitarle!"));
            }
        }
    }

    // --------------------------------------------------------------- tarjetas

    private void avisosDeTarjetas(LocalDate hoy, List<Aviso> avisos) throws Exception {
        for (Tarjeta t : tarjetaDAO.listarActivas()) {
            // Corte de hoy: total acumulado del periodo que se cierra
            LocalDate corte = t.corteEn(YearMonth.from(hoy));
            if (corte.equals(hoy) && tarjetaDAO.reclamarAvisoCorte(t.getId(), hoy)) {
                BigDecimal total = gastoDAO.totalPeriodo(t.getId(), t.corteAnterior(corte), corte);
                avisos.add(new Aviso("Corte de " + t.getNombre(),
                        "Total a pagar: " + Fechas.dinero(total)
                                + " · Fecha límite: " + Fechas.largo(t.pagoDeCorte(corte)).toLowerCase(Fechas.ES)));
            }

            // Fecha límite de hoy: puede venir de un corte de hasta 3 meses atrás (días de gracia)
            for (int meses = 0; meses <= 3; meses++) {
                LocalDate corteOrigen = t.corteEn(YearMonth.from(hoy).minusMonths(meses));
                if (t.pagoDeCorte(corteOrigen).equals(hoy) && tarjetaDAO.reclamarAvisoPago(t.getId(), hoy)) {
                    BigDecimal total = gastoDAO.totalPeriodo(t.getId(), t.corteAnterior(corteOrigen), corteOrigen);
                    avisos.add(new Aviso("Hoy vence el pago de " + t.getNombre(),
                            "Monto del corte del " + Fechas.diaMes(corteOrigen) + ": " + Fechas.dinero(total)));
                    break;
                }
            }
        }
    }
}
