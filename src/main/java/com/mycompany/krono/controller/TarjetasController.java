package com.mycompany.krono.controller;

import com.mycompany.krono.db.ConexionDB;
import com.mycompany.krono.model.Gasto;
import com.mycompany.krono.model.Tarjeta;
import com.mycompany.krono.service.TarjetaService;
import com.mycompany.krono.service.TarjetaService.Periodo;
import com.mycompany.krono.service.TarjetaService.ResumenTarjeta;
import com.mycompany.krono.ui.GastoCell;
import com.mycompany.krono.ui.TarjetaCell;
import com.mycompany.krono.util.Cambios;
import com.mycompany.krono.util.Dialogos;
import com.mycompany.krono.util.Fechas;
import com.mycompany.krono.util.Iconos;
import com.mycompany.krono.util.Tareas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Vista Tarjetas y Gastos: lista de tarjetas con el total del periodo actual y, de la elegida,
 * los gastos del periodo, navegables hacia atrás y hacia adelante.
 */
public class TarjetasController {

    @FXML private Label lblConteoTarjetas;
    @FXML private ListView<ResumenTarjeta> lstTarjetas;
    @FXML private VBox panelDetalle;
    @FXML private VBox panelVacio;
    @FXML private Label lblNombre;
    @FXML private Label lblConfiguracion;
    @FXML private Button btnPeriodoAnterior;
    @FXML private Button btnPeriodoSiguiente;
    @FXML private Label lblPeriodo;
    @FXML private Label lblPeriodoActual;
    @FXML private Button btnPeriodoHoy;
    @FXML private Label lblTotal;
    @FXML private Label lblCorte;
    @FXML private Label lblPago;
    @FXML private Label lblNumGastos;
    @FXML private ListView<Gasto> lstGastos;

    private final TarjetaService servicio = new TarjetaService();
    private Tarjeta tarjeta;
    private Periodo periodo;
    private long generacion;

    @FXML
    private void initialize() {
        btnPeriodoAnterior.setGraphic(Iconos.crear(Iconos.ANTERIOR, 18));
        btnPeriodoAnterior.setTooltip(new Tooltip("Periodo anterior"));
        btnPeriodoSiguiente.setGraphic(Iconos.crear(Iconos.SIGUIENTE, 18));
        btnPeriodoSiguiente.setTooltip(new Tooltip("Periodo siguiente"));

        lstTarjetas.setCellFactory(lv -> new TarjetaCell());
        lstTarjetas.getSelectionModel().selectedItemProperty().addListener((obs, a, r) -> {
            if (r != null && (tarjeta == null || !Objects.equals(tarjeta.getId(), r.tarjeta().getId()))) {
                tarjeta = r.tarjeta();
                periodo = r.periodo();
                cargarPeriodo();
            }
        });

        lstGastos.setCellFactory(lv -> new GastoCell(
                g -> Acciones.editarGasto(ventana(), tarjetasActuales(), g),
                g -> Acciones.eliminarGasto(ventana(), g)));
        Label vacio = new Label("Sin gastos en este periodo.\nUsa «Registrar gasto» para agregar uno.");
        vacio.getStyleClass().add("empty-text");
        lstGastos.setPlaceholder(vacio);

        Cambios.escuchar(this::recargar);
        recargar();
    }

    // ================================================================== datos

    public void recargar() {
        Tareas.ejecutar(() -> servicio.resumenes(LocalDate.now()), this::mostrarTarjetas, this::errorBD);
    }

    private void mostrarTarjetas(List<ResumenTarjeta> resumenes) {
        lstTarjetas.getItems().setAll(resumenes);
        long activas = resumenes.stream().filter(r -> r.tarjeta().isActiva()).count();
        lblConteoTarjetas.setText(resumenes.isEmpty() ? "Ninguna registrada"
                : activas + (activas == 1 ? " activa" : " activas")
                + (resumenes.size() > activas ? " · " + (resumenes.size() - activas) + " inactivas" : ""));

        boolean hay = !resumenes.isEmpty();
        panelDetalle.setVisible(hay);
        panelVacio.setVisible(!hay);
        if (!hay) {
            tarjeta = null;
            return;
        }

        // Conserva la tarjeta y el periodo elegidos si siguen existiendo
        ResumenTarjeta elegida = resumenes.stream()
                .filter(r -> tarjeta != null && Objects.equals(r.tarjeta().getId(), tarjeta.getId()))
                .findFirst().orElse(resumenes.get(0));
        boolean misma = tarjeta != null && Objects.equals(tarjeta.getId(), elegida.tarjeta().getId());
        Periodo anterior = periodo;
        tarjeta = elegida.tarjeta();
        periodo = misma && anterior != null
                ? TarjetaService.periodoDeCorte(tarjeta, tarjeta.corteEn(java.time.YearMonth.from(anterior.corte())))
                : elegida.periodo();
        lstTarjetas.getSelectionModel().select(elegida);
        cargarPeriodo();
    }

    private void cargarPeriodo() {
        if (tarjeta == null || periodo == null) {
            return;
        }
        Tarjeta t = tarjeta;
        Periodo p = periodo;
        long gen = ++generacion;

        lblNombre.setText(t.getNombre() + (t.isActiva() ? "" : " (inactiva)"));
        lblConfiguracion.setText(TarjetaCell.describir(t)
                + (t.getNotas() == null || t.getNotas().isBlank() ? "" : " · " + t.getNotas()));
        lblPeriodo.setText("Periodo " + Fechas.corto(p.inicio()) + " – " + Fechas.corto(p.corte()) + " "
                + p.corte().getYear());
        boolean actual = p.contiene(LocalDate.now());
        lblPeriodoActual.setVisible(actual);
        lblPeriodoActual.setManaged(actual);
        btnPeriodoHoy.setDisable(actual);
        lblCorte.setText(Fechas.corto(p.corte()));
        lblPago.setText(Fechas.corto(p.pago()));

        Tareas.ejecutar(() -> new Detalle(servicio.gastos(t, p), servicio.total(t, p)),
                d -> {
                    if (gen != generacion) {
                        return;
                    }
                    lstGastos.getItems().setAll(d.gastos());
                    lblTotal.setText(Fechas.dinero(d.total()));
                    lblNumGastos.setText(String.valueOf(d.gastos().size()));
                },
                this::errorBD);
    }

    private record Detalle(List<Gasto> gastos, BigDecimal total) {
    }

    // ================================================================ acciones

    @FXML
    private void onNuevaTarjeta() {
        Acciones.nuevaTarjeta(ventana(), null);
    }

    @FXML
    private void onEditarTarjeta() {
        if (tarjeta != null) {
            Acciones.editarTarjeta(ventana(), tarjeta);
        }
    }

    @FXML
    private void onEliminarTarjeta() {
        if (tarjeta != null) {
            Acciones.eliminarTarjeta(ventana(), tarjeta);
        }
    }

    @FXML
    private void onNuevoGasto() {
        List<Tarjeta> activas = tarjetasActuales().stream().filter(Tarjeta::isActiva).toList();
        Tarjeta pre = tarjeta != null && tarjeta.isActiva() ? tarjeta : null;
        Acciones.nuevoGasto(ventana(), activas.isEmpty() ? tarjetasActuales() : activas, pre);
    }

    @FXML
    private void onPeriodoAnterior() {
        moverPeriodo(-1);
    }

    @FXML
    private void onPeriodoSiguiente() {
        moverPeriodo(1);
    }

    @FXML
    private void onPeriodoHoy() {
        if (tarjeta != null) {
            periodo = TarjetaService.periodoQueContiene(tarjeta, LocalDate.now());
            cargarPeriodo();
        }
    }

    private void moverPeriodo(int meses) {
        if (tarjeta != null && periodo != null) {
            periodo = TarjetaService.desplazar(tarjeta, periodo, meses);
            cargarPeriodo();
        }
    }

    // =============================================================== utilidades

    private List<Tarjeta> tarjetasActuales() {
        return lstTarjetas.getItems().stream().map(ResumenTarjeta::tarjeta).toList();
    }

    private void errorBD(Throwable error) {
        Dialogos.error(ventana(), "No se pudieron cargar las tarjetas", ConexionDB.describir(error));
    }

    private Window ventana() {
        return lstTarjetas.getScene() == null ? null : lstTarjetas.getScene().getWindow();
    }
}
