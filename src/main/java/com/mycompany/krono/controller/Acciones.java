package com.mycompany.krono.controller;

import com.mycompany.krono.dao.CumpleanosDAO;
import com.mycompany.krono.dao.EventoDAO;
import com.mycompany.krono.dao.GastoDAO;
import com.mycompany.krono.dao.TarjetaDAO;
import com.mycompany.krono.db.ConexionDB;
import com.mycompany.krono.model.Cumpleanos;
import com.mycompany.krono.model.Evento;
import com.mycompany.krono.model.Gasto;
import com.mycompany.krono.model.Tarjeta;
import com.mycompany.krono.model.TipoEvento;
import com.mycompany.krono.ui.form.CumpleanosForm;
import com.mycompany.krono.ui.form.EventoForm;
import com.mycompany.krono.ui.form.GastoForm;
import com.mycompany.krono.ui.form.TarjetaForm;
import com.mycompany.krono.util.Cambios;
import com.mycompany.krono.util.Dialogos;
import com.mycompany.krono.util.Fechas;
import com.mycompany.krono.util.Tareas;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javafx.stage.Window;

/**
 * Altas, ediciones y bajas compartidas por las vistas: abre el formulario, guarda en segundo
 * plano y, al terminar, avisa a todas las vistas para que se actualicen (Cambios).
 */
public final class Acciones {

    @FunctionalInterface
    interface OperacionBD {
        void ejecutar() throws SQLException;
    }

    private static final EventoDAO EVENTOS = new EventoDAO();
    private static final CumpleanosDAO CUMPLEANOS = new CumpleanosDAO();
    private static final TarjetaDAO TARJETAS = new TarjetaDAO();
    private static final GastoDAO GASTOS = new GastoDAO();

    private Acciones() {
    }

    // ---------------------------------------------------------------- eventos

    public static void nuevoEvento(Window w, LocalDate fecha, TipoEvento tipo) {
        Evento e = new Evento();
        e.setTipo(tipo);
        e.setFecha(fecha);
        e.setAlarmaActiva(tipo == TipoEvento.JUNTA);
        new EventoForm(w, e).showAndWait().ifPresent(n -> guardar(w, () -> EVENTOS.insertar(n)));
    }

    public static void editarEvento(Window w, Evento e) {
        new EventoForm(w, e).showAndWait().ifPresent(n -> guardar(w, () -> EVENTOS.actualizar(n)));
    }

    // ------------------------------------------------------------- cumpleaños

    public static void nuevoCumpleanos(Window w, LocalDate fecha) {
        Cumpleanos c = new Cumpleanos();
        c.setFechaNacimiento(fecha);
        new CumpleanosForm(w, c).showAndWait().ifPresent(n -> guardar(w, () -> CUMPLEANOS.insertar(n)));
    }

    public static void editarCumpleanos(Window w, Cumpleanos c) {
        new CumpleanosForm(w, c).showAndWait().ifPresent(n -> guardar(w, () -> CUMPLEANOS.actualizar(n)));
    }

    // --------------------------------------------------------------- tarjetas

    /** @param fecha día que se usa como día de corte inicial (o null para hoy) */
    public static void nuevaTarjeta(Window w, LocalDate fecha) {
        Tarjeta t = new Tarjeta();
        t.setDiaCorte((fecha == null ? LocalDate.now() : fecha).getDayOfMonth());
        new TarjetaForm(w, t).showAndWait().ifPresent(n -> guardar(w, () -> TARJETAS.insertar(n)));
    }

    public static void editarTarjeta(Window w, Tarjeta t) {
        new TarjetaForm(w, t).showAndWait().ifPresent(n -> guardar(w, () -> TARJETAS.actualizar(n)));
    }

    public static void eliminarTarjeta(Window w, Tarjeta t) {
        if (Dialogos.confirmar(w, "¿Eliminar la tarjeta " + t.getNombre() + "?",
                "Se eliminarán también todos sus gastos registrados, cortes y pagos.\n"
                        + "Si solo dejaste de usarla, mejor edítala y desmarca «Activa».", "Eliminar")) {
            guardar(w, () -> TARJETAS.eliminar(t.getId()));
        }
    }

    // ----------------------------------------------------------------- gastos

    public static void nuevoGasto(Window w, List<Tarjeta> tarjetas, Tarjeta preseleccion) {
        if (tarjetas.isEmpty()) {
            Dialogos.info(w, "Primero registra una tarjeta", "Los gastos se asocian a una tarjeta de crédito.");
            return;
        }
        Gasto g = new Gasto();
        g.setTarjetaId(preseleccion == null ? tarjetas.get(0).getId() : preseleccion.getId());
        g.setFecha(LocalDate.now());
        new GastoForm(w, g, tarjetas).showAndWait().ifPresent(n -> guardar(w, () -> GASTOS.insertar(n)));
    }

    public static void editarGasto(Window w, List<Tarjeta> tarjetas, Gasto g) {
        new GastoForm(w, g, tarjetas).showAndWait().ifPresent(n -> guardar(w, () -> GASTOS.actualizar(n)));
    }

    public static void eliminarGasto(Window w, Gasto g) {
        if (Dialogos.confirmar(w, "¿Eliminar el gasto?",
                "«" + g.getConcepto() + "» por " + Fechas.dinero(g.getMonto()) + " del " + Fechas.diaMes(g.getFecha())
                        + ".\nEl total del periodo se recalculará.", "Eliminar")) {
            guardar(w, () -> GASTOS.eliminar(g.getId()));
        }
    }

    // ------------------------------------------------------------------ apoyo

    static void guardar(Window w, OperacionBD operacion) {
        Tareas.ejecutar(() -> {
            operacion.ejecutar();
            return null;
        }, r -> Cambios.notificar(),
                error -> Dialogos.error(w, "No se pudieron guardar los cambios", ConexionDB.describir(error)));
    }
}
