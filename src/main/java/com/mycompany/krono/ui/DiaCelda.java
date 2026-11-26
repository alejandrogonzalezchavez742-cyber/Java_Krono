package com.mycompany.krono.ui;

import com.mycompany.krono.model.Ocurrencia;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Celda de un día en la cuadrícula: número en negrita y debajo, en fila, una marca por evento
 * (punto de color + ícono de su categoría).
 *
 * Se muestran hasta {@value #MAX_MARCAS} marcas y un contador "+N" con el resto. Si la celda es
 * angosta (ventana pequeña) se muestran las que caben sin encimarse, y el "+N" crece.
 */
public class DiaCelda extends VBox {

    public static final int MAX_MARCAS = 4;

    private static final double RADIO_PUNTO = 3.5;
    private static final double TAMANO_ICONO = 11;
    private static final double HUECO = 4;
    /** Ancho reservado para el contador "+N". */
    private static final double ANCHO_CONTADOR = 18;
    /** Relleno (4 px) y borde (2 px) de la celda a cada lado. */
    private static final double MARGEN_CELDA = 12;

    private static final PseudoClass FUERA_DE_MES = PseudoClass.getPseudoClass("out-month");
    private static final PseudoClass HOY = PseudoClass.getPseudoClass("today");
    private static final PseudoClass SELECCIONADA = PseudoClass.getPseudoClass("selected");

    private final Label numero = new Label();
    private final HBox marcas = new HBox(HUECO);
    private final Tooltip tooltip = new Tooltip();
    private LocalDate fecha;
    private List<Ocurrencia> ocurrencias = List.of();
    private int visiblesPintadas = -1;

    public DiaCelda() {
        getStyleClass().add("day-cell");
        setAlignment(Pos.CENTER);
        setSpacing(6);
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        numero.getStyleClass().add("day-number");
        marcas.setAlignment(Pos.CENTER);
        marcas.setMinHeight(TAMANO_ICONO + 1);
        marcas.setMaxHeight(TAMANO_ICONO + 1);
        getChildren().addAll(numero, marcas);

        tooltip.setShowDelay(Duration.millis(400));
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(320);

        // Al cambiar el ancho de la ventana, recalcula cuántas marcas caben
        widthProperty().addListener((obs, a, b) -> {
            if (cuantasVisibles() != visiblesPintadas) {
                pintarMarcas();
            }
        });
    }

    public LocalDate getFecha() {
        return fecha;
    }

    /** @param ocurrencias eventos del día ya filtrados y ordenados (prioritarios primero) */
    public void mostrar(LocalDate fecha, boolean delMes, boolean hoy, boolean seleccionada, List<Ocurrencia> ocurrencias) {
        this.fecha = fecha;
        this.ocurrencias = ocurrencias;
        numero.setText(String.valueOf(fecha.getDayOfMonth()));
        pseudoClassStateChanged(FUERA_DE_MES, !delMes);
        pseudoClassStateChanged(HOY, hoy);
        pseudoClassStateChanged(SELECCIONADA, seleccionada);
        pintarMarcas();

        if (ocurrencias.isEmpty()) {
            Tooltip.uninstall(this, tooltip);
        } else {
            tooltip.setText(ocurrencias.stream()
                    .map(o -> (o.prioritaria() ? "★ " : "• ") + o.titulo())
                    .collect(Collectors.joining("\n")));
            Tooltip.install(this, tooltip);
        }
    }

    private void pintarMarcas() {
        int visibles = cuantasVisibles();
        visiblesPintadas = visibles;
        marcas.getChildren().clear();
        for (int i = 0; i < visibles; i++) {
            marcas.getChildren().add(MarcaCategoria.de(ocurrencias.get(i), RADIO_PUNTO, TAMANO_ICONO));
        }
        int resto = ocurrencias.size() - visibles;
        if (resto > 0) {
            Label mas = new Label("+" + resto);
            mas.getStyleClass().add("dot-more");
            mas.setMinWidth(Label.USE_PREF_SIZE);
            marcas.getChildren().add(mas);
        }
    }

    /** Marcas que caben a lo ancho sin encimarse, como máximo {@value #MAX_MARCAS}. */
    private int cuantasVisibles() {
        int total = ocurrencias.size();
        if (total == 0) {
            return 0;
        }
        double ancho = getWidth() - MARGEN_CELDA;
        if (ancho <= 0) {
            return Math.min(total, MAX_MARCAS);      // aún sin medir: se corrige al conocer el ancho
        }
        double paso = MarcaCategoria.ancho(RADIO_PUNTO, TAMANO_ICONO) + HUECO;
        int caben = (int) ((ancho + HUECO) / paso);
        if (total <= Math.min(caben, MAX_MARCAS)) {
            return total;
        }
        // Hay que mostrar "+N": se descuenta su espacio
        int conContador = (int) ((ancho - ANCHO_CONTADOR) / paso);
        return Math.max(1, Math.min(MAX_MARCAS, conContador));
    }
}
