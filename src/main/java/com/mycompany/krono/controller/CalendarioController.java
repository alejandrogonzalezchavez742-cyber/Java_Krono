package com.mycompany.krono.controller;

import com.mycompany.krono.db.ConexionDB;
import com.mycompany.krono.model.CategoriaCalendario;
import com.mycompany.krono.model.Cumpleanos;
import com.mycompany.krono.model.Evento;
import com.mycompany.krono.model.FiltroCalendario;
import com.mycompany.krono.model.Ocurrencia;
import com.mycompany.krono.model.Tarjeta;
import com.mycompany.krono.model.TipoEvento;
import com.mycompany.krono.util.Cambios;
import com.mycompany.krono.service.CalendarioService;
import com.mycompany.krono.ui.DiaCelda;
import com.mycompany.krono.ui.MarcaCategoria;
import com.mycompany.krono.ui.OcurrenciaCell;
import com.mycompany.krono.ui.ProximoCell;
import com.mycompany.krono.util.Dialogos;
import com.mycompany.krono.util.Fechas;
import com.mycompany.krono.util.Iconos;
import com.mycompany.krono.util.Tareas;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import javafx.util.StringConverter;

/**
 * Vista Calendario: resumen del día, controles de navegación, cuadrícula de 6 semanas
 * y panel lateral con el detalle del día y los próximos 15 días.
 */
public class CalendarioController {

    private static final String[] ENCABEZADOS = {"Do", "Lu", "Ma", "Mi", "Ju", "Vi", "Sá"};
    private static final int CELDAS = 42;            // 6 semanas: cualquier mes cabe
    private static final int DIAS_PROXIMOS = 15;
    private static final int RANGO_ANIOS = 10;

    @FXML private Label lblDiaSemana;
    @FXML private Label lblDiaNumero;
    @FXML private Label lblMesAnio;
    @FXML private Label lblSemana;
    @FXML private Label lblResumen;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Button btnHoy;
    @FXML private Button btnNuevo;
    @FXML private ComboBox<Month> cmbMes;
    @FXML private ComboBox<Integer> cmbAnio;
    @FXML private ComboBox<FiltroCalendario> cmbFiltro;
    @FXML private GridPane gridCalendario;
    @FXML private FlowPane boxLeyenda;
    @FXML private Label lblPanelFecha;
    @FXML private ListView<Ocurrencia> lstDia;
    @FXML private ListView<Ocurrencia> lstProximos;

    private final CalendarioService servicio = new CalendarioService();
    private final DiaCelda[] celdas = new DiaCelda[CELDAS];

    private YearMonth mesVisible;
    private LocalDate seleccion;
    private Map<LocalDate, List<Ocurrencia>> datosMes = Map.of();
    private Map<LocalDate, List<Ocurrencia>> datosProximos = Map.of();

    /** Evita que los listeners de los ComboBox reaccionen cuando el código los cambia. */
    private boolean sincronizando;
    /** Descarta respuestas de cargas viejas si el usuario cambió de mes antes de que llegaran. */
    private long generacion;

    @FXML
    private void initialize() {
        btnAnterior.setGraphic(Iconos.crear(Iconos.ANTERIOR, 18));
        btnAnterior.setTooltip(new Tooltip("Mes anterior (RePág)"));
        btnSiguiente.setGraphic(Iconos.crear(Iconos.SIGUIENTE, 18));
        btnSiguiente.setTooltip(new Tooltip("Mes siguiente (AvPág)"));
        btnHoy.setTooltip(new Tooltip("Ir a hoy (Inicio)"));
        btnNuevo.setGraphic(Iconos.crear(Iconos.AGREGAR, 16));

        construirCuadricula();
        configurarCombos();
        configurarListas();
        construirLeyenda();

        seleccion = LocalDate.now();
        mesVisible = YearMonth.from(seleccion);
        pintar();
        recargar();
        Cambios.escuchar(this::recargar);

        // Foco inicial en la cuadrícula: las flechas del teclado mueven el día de inmediato
        gridCalendario.sceneProperty().addListener((obs, vieja, escena) -> {
            if (escena != null) {
                Platform.runLater(gridCalendario::requestFocus);
            }
        });
    }

    // ============================================================ construcción

    private void construirCuadricula() {
        for (int c = 0; c < 7; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHgrow(Priority.ALWAYS);
            gridCalendario.getColumnConstraints().add(cc);
        }
        RowConstraints encabezado = new RowConstraints(26);
        encabezado.setVgrow(Priority.NEVER);
        gridCalendario.getRowConstraints().add(encabezado);
        for (int f = 0; f < 6; f++) {
            RowConstraints rc = new RowConstraints(48, 72, Double.MAX_VALUE);
            rc.setVgrow(Priority.ALWAYS);
            gridCalendario.getRowConstraints().add(rc);
        }

        for (int c = 0; c < 7; c++) {
            Label l = new Label(ENCABEZADOS[c]);
            l.getStyleClass().add("weekday-header");
            l.setMaxWidth(Double.MAX_VALUE);
            l.setAlignment(Pos.CENTER);
            gridCalendario.add(l, c, 0);
        }

        for (int i = 0; i < CELDAS; i++) {
            DiaCelda celda = new DiaCelda();
            celda.setOnMouseClicked(e -> {
                if (celda.getFecha() == null) {
                    return;
                }
                irA(celda.getFecha());
                gridCalendario.requestFocus();
                if (e.getClickCount() == 2) {
                    menuNuevo(celda.getFecha()).show(celda, e.getScreenX(), e.getScreenY());
                }
            });
            celdas[i] = celda;
            gridCalendario.add(celda, i % 7, 1 + i / 7);
        }

        gridCalendario.setFocusTraversable(true);
        gridCalendario.addEventFilter(KeyEvent.KEY_PRESSED, this::teclado);
    }

    private void configurarCombos() {
        cmbMes.getItems().setAll(Month.values());
        cmbMes.setConverter(new StringConverter<>() {
            @Override
            public String toString(Month m) {
                return m == null ? "" : Fechas.mes(m);
            }

            @Override
            public Month fromString(String s) {
                return null;
            }
        });
        cmbMes.valueProperty().addListener((obs, a, m) -> {
            if (!sincronizando && m != null) {
                irA(Fechas.ajustarDia(YearMonth.of(mesVisible.getYear(), m), seleccion.getDayOfMonth()));
            }
        });

        cmbAnio.valueProperty().addListener((obs, a, anio) -> {
            if (!sincronizando && anio != null) {
                irA(Fechas.ajustarDia(YearMonth.of(anio, mesVisible.getMonth()), seleccion.getDayOfMonth()));
            }
        });

        cmbFiltro.getItems().setAll(FiltroCalendario.values());
        cmbFiltro.setValue(FiltroCalendario.TODOS);
        cmbFiltro.valueProperty().addListener((obs, a, f) -> pintar());
    }

    private void configurarListas() {
        lstDia.setCellFactory(lv -> new OcurrenciaCell(this::completar, this::editar, this::eliminar));
        lstDia.setPlaceholder(textoVacio("No hay eventos este día.\nHaz doble clic en un día para agregar uno."));
        lstDia.setFocusTraversable(false);

        lstProximos.setCellFactory(lv -> new ProximoCell(o -> irA(o.fecha())));
        lstProximos.setPlaceholder(textoVacio("Nada programado en los próximos 15 días."));
        lstProximos.setFocusTraversable(false);
    }

    /** Leyenda: punto + ícono de cada categoría. Los asuntos se separan en importantes (pin) y prioritarios (estrella). */
    private void construirLeyenda() {
        agregarLeyenda(CategoriaCalendario.TAREA, true, "Prioritarias");
        for (CategoriaCalendario cat : CategoriaCalendario.values()) {
            agregarLeyenda(cat, false, cat.nombre());
        }
    }

    private void agregarLeyenda(CategoriaCalendario cat, boolean prioritaria, String texto) {
        Label l = new Label(texto, MarcaCategoria.crear(cat, prioritaria, 4, 12));
        l.getStyleClass().add("legend-item");
        l.setGraphicTextGap(6);
        l.setMinWidth(Label.USE_PREF_SIZE);   // nunca "Priori…": mejor pasar a la siguiente línea
        boxLeyenda.getChildren().add(l);
    }

    // ============================================================== navegación

    @FXML
    private void onAnterior() {
        irA(Fechas.ajustarDia(mesVisible.minusMonths(1), seleccion.getDayOfMonth()));
    }

    @FXML
    private void onSiguiente() {
        irA(Fechas.ajustarDia(mesVisible.plusMonths(1), seleccion.getDayOfMonth()));
    }

    @FXML
    private void onHoy() {
        irA(LocalDate.now());
    }

    @FXML
    private void onNuevo() {
        menuNuevo(seleccion).show(btnNuevo, Side.BOTTOM, 0, 4);
    }

    /** Selecciona un día. Si es de otro mes, cambia de mes y recarga. */
    public void irA(LocalDate fecha) {
        YearMonth mes = YearMonth.from(fecha);
        boolean cambiaMes = !mes.equals(mesVisible);
        seleccion = fecha;
        mesVisible = mes;
        pintar();
        if (cambiaMes) {
            recargar();
        }
    }

    private void teclado(KeyEvent e) {
        LocalDate destino = switch (e.getCode()) {
            case LEFT -> seleccion.minusDays(1);
            case RIGHT -> seleccion.plusDays(1);
            case UP -> seleccion.minusWeeks(1);
            case DOWN -> seleccion.plusWeeks(1);
            case PAGE_UP -> seleccion.minusMonths(1);
            case PAGE_DOWN -> seleccion.plusMonths(1);
            case HOME -> LocalDate.now();
            default -> null;
        };
        if (destino != null) {
            irA(destino);
            e.consume();
        }
    }

    // ================================================================== datos

    /** Vuelve a leer la base de datos para el mes visible y los próximos 15 días. */
    public void recargar() {
        long gen = ++generacion;
        LocalDate inicio = inicioCuadricula();
        LocalDate fin = inicio.plusDays(CELDAS - 1);
        LocalDate hoy = LocalDate.now();

        Tareas.ejecutar(
                () -> new Datos(servicio.cargar(inicio, fin),
                        servicio.cargar(hoy.plusDays(1), hoy.plusDays(DIAS_PROXIMOS))),
                datos -> {
                    if (gen != generacion) {
                        return;
                    }
                    datosMes = datos.mes();
                    datosProximos = datos.proximos();
                    pintar();
                },
                error -> {
                    if (gen == generacion) {
                        Dialogos.error(ventana(), "No se pudieron cargar los eventos", ConexionDB.describir(error));
                    }
                });
    }

    private record Datos(Map<LocalDate, List<Ocurrencia>> mes, Map<LocalDate, List<Ocurrencia>> proximos) {
    }

    // ================================================================ pintado

    private void pintar() {
        sincronizarCombos();
        pintarCuadricula();
        pintarResumen();
        pintarPanel();
    }

    private void sincronizarCombos() {
        sincronizando = true;
        try {
            int anio = mesVisible.getYear();
            if (!cmbAnio.getItems().contains(anio)) {
                int centro = anio;
                cmbAnio.getItems().setAll(IntStream.rangeClosed(centro - RANGO_ANIOS, centro + RANGO_ANIOS)
                        .boxed().toList());
            }
            cmbAnio.setValue(anio);
            cmbMes.setValue(mesVisible.getMonth());
        } finally {
            sincronizando = false;
        }
    }

    private void pintarCuadricula() {
        LocalDate inicio = inicioCuadricula();
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i < CELDAS; i++) {
            LocalDate f = inicio.plusDays(i);
            celdas[i].mostrar(f, YearMonth.from(f).equals(mesVisible), f.equals(hoy), f.equals(seleccion),
                    filtrar(datosMes.getOrDefault(f, List.of())));
        }
    }

    private void pintarResumen() {
        List<Ocurrencia> delDia = filtrar(datosMes.getOrDefault(seleccion, List.of()));
        lblDiaSemana.setText(Fechas.diaAbrev(seleccion));
        lblDiaNumero.setText(String.valueOf(seleccion.getDayOfMonth()));
        lblMesAnio.setText(Fechas.mes(mesVisible.getMonth()) + " " + mesVisible.getYear());
        lblSemana.setText("Semana " + Fechas.semana(seleccion) + " · " + Fechas.largo(seleccion));
        lblResumen.setText(resumen(delDia));
    }

    private void pintarPanel() {
        lblPanelFecha.setText(Fechas.largo(seleccion));
        lstDia.getItems().setAll(filtrar(datosMes.getOrDefault(seleccion, List.of())));

        List<Ocurrencia> proximos = new ArrayList<>();
        datosProximos.values().forEach(l -> proximos.addAll(filtrar(l)));   // TreeMap: ya en orden de fecha
        lstProximos.getItems().setAll(proximos);
    }

    /** "4 eventos · 1 prioritaria · 1 junta · 1 corte ($2,269.40)" */
    static String resumen(List<Ocurrencia> dia) {
        if (dia.isEmpty()) {
            return "Sin eventos para este día.";
        }
        List<String> partes = new ArrayList<>();
        partes.add(dia.size() + (dia.size() == 1 ? " evento" : " eventos"));
        contar(partes, dia.stream().filter(Ocurrencia::prioritaria).count(), "prioritaria", "prioritarias");
        contar(partes, cuenta(dia, CategoriaCalendario.JUNTA), "junta", "juntas");
        contar(partes, cuenta(dia, CategoriaCalendario.CUMPLEANOS), "cumpleaños", "cumpleaños");
        dia.stream().filter(o -> o.categoria() == CategoriaCalendario.CORTE)
                .forEach(o -> partes.add("corte " + Fechas.dinero(o.monto())));
        dia.stream().filter(o -> o.categoria() == CategoriaCalendario.PAGO)
                .forEach(o -> partes.add("pagar " + Fechas.dinero(o.monto())));
        contar(partes, dia.stream().filter(Ocurrencia::completada).count(), "completada", "completadas");
        return String.join(" · ", partes);
    }

    private static long cuenta(List<Ocurrencia> dia, CategoriaCalendario cat) {
        return dia.stream().filter(o -> o.categoria() == cat).count();
    }

    private static void contar(List<String> partes, long n, String singular, String plural) {
        if (n > 0) {
            partes.add(n + " " + (n == 1 ? singular : plural));
        }
    }

    // ================================================================ acciones

    private void completar(Ocurrencia o) {
        if (o.origen() instanceof Evento e) {
            Tareas.ejecutar(() -> {
                servicio.alternarCompletada(e);
                return null;
            }, r -> Cambios.notificar(), this::errorBD);
        }
    }

    private void eliminar(Ocurrencia o) {
        String mensaje = switch (o.origen()) {
            case Evento e -> "Se eliminará «" + e.getTitulo() + "».";
            case Cumpleanos c -> "Se eliminará el cumpleaños de " + c.getNombre() + " de todos los años.";
            case Tarjeta t -> "Se eliminará la tarjeta " + t.getNombre()
                    + " junto con todos sus gastos registrados, cortes y pagos.";
            default -> "Se eliminará este elemento.";
        };
        if (Dialogos.confirmar(ventana(), "¿Eliminar?", mensaje + "\nEsta acción no se puede deshacer.", "Eliminar")) {
            Tareas.ejecutar(() -> {
                servicio.eliminar(o);
                return null;
            }, r -> Cambios.notificar(), this::errorBD);
        }
    }

    /** Abre el formulario que corresponde al origen: evento, cumpleaños o tarjeta (cortes y pagos). */
    private void editar(Ocurrencia o) {
        switch (o.origen()) {
            case Evento e -> Acciones.editarEvento(ventana(), e);
            case Cumpleanos c -> Acciones.editarCumpleanos(ventana(), c);
            case Tarjeta t -> Acciones.editarTarjeta(ventana(), t);
            default -> { }
        }
    }

    /** Menú "Nuevo" con los cuatro tipos, cada uno con el punto de su color. */
    private ContextMenu menuNuevo(LocalDate fecha) {
        ContextMenu menu = new ContextMenu(
                opcion("Asunto / Tarea", CategoriaCalendario.TAREA,
                        () -> Acciones.nuevoEvento(ventana(), fecha, TipoEvento.TAREA)),
                opcion("Junta / Reunión", CategoriaCalendario.JUNTA,
                        () -> Acciones.nuevoEvento(ventana(), fecha, TipoEvento.JUNTA)),
                opcion("Cumpleaños", CategoriaCalendario.CUMPLEANOS,
                        () -> Acciones.nuevoCumpleanos(ventana(), fecha)),
                opcion("Tarjeta de crédito", CategoriaCalendario.CORTE,
                        () -> Acciones.nuevaTarjeta(ventana(), fecha)));
        menu.getStyleClass().add("krono-menu");
        return menu;
    }

    private static MenuItem opcion(String texto, CategoriaCalendario categoria, Runnable accion) {
        Circle punto = new Circle(4);
        punto.getStyleClass().addAll("dot", categoria.claseCss());
        MenuItem item = new MenuItem(texto, punto);
        item.setOnAction(e -> accion.run());
        return item;
    }

    private void errorBD(Throwable error) {
        Dialogos.error(ventana(), "No se pudo completar la operación", ConexionDB.describir(error));
    }

    // =============================================================== utilidades

    /** Domingo de la semana que contiene el día 1 del mes visible. */
    private LocalDate inicioCuadricula() {
        LocalDate primero = mesVisible.atDay(1);
        return primero.minusDays(primero.getDayOfWeek().getValue() % 7);
    }

    private List<Ocurrencia> filtrar(List<Ocurrencia> lista) {
        FiltroCalendario f = cmbFiltro.getValue();
        return f == null || f == FiltroCalendario.TODOS ? lista : lista.stream().filter(f::acepta).toList();
    }

    private static Label textoVacio(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("empty-text");
        l.setWrapText(true);
        return l;
    }

    private Window ventana() {
        return gridCalendario.getScene() == null ? null : gridCalendario.getScene().getWindow();
    }
}
