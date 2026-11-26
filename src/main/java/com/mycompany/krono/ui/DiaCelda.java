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
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Celda de un día en la cuadrícula: número en negrita y debajo un punto de color por evento,
 * en fila. A partir de 5 eventos muestra 4 puntos y un contador "+N".
 */
public class DiaCelda extends VBox {

    public static final int MAX_PUNTOS = 4;

    private static final PseudoClass FUERA_DE_MES = PseudoClass.getPseudoClass("out-month");
    private static final PseudoClass HOY = PseudoClass.getPseudoClass("today");
    private static final PseudoClass SELECCIONADA = PseudoClass.getPseudoClass("selected");

    private final Label numero = new Label();
    private final HBox puntos = new HBox(4);
    private final Tooltip tooltip = new Tooltip();
    private LocalDate fecha;

    public DiaCelda() {
        getStyleClass().add("day-cell");
        setAlignment(Pos.CENTER);
        setSpacing(6);
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        numero.getStyleClass().add("day-number");
        puntos.setAlignment(Pos.CENTER);
        puntos.setMinHeight(8);
        puntos.setMaxHeight(8);
        getChildren().addAll(numero, puntos);

        tooltip.setShowDelay(Duration.millis(400));
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(320);
    }

    public LocalDate getFecha() {
        return fecha;
    }

    /** @param ocurrencias eventos del día ya filtrados y ordenados */
    public void mostrar(LocalDate fecha, boolean delMes, boolean hoy, boolean seleccionada, List<Ocurrencia> ocurrencias) {
        this.fecha = fecha;
        numero.setText(String.valueOf(fecha.getDayOfMonth()));
        pseudoClassStateChanged(FUERA_DE_MES, !delMes);
        pseudoClassStateChanged(HOY, hoy);
        pseudoClassStateChanged(SELECCIONADA, seleccionada);

        puntos.getChildren().clear();
        int visibles = ocurrencias.size() > MAX_PUNTOS ? MAX_PUNTOS : ocurrencias.size();
        for (int i = 0; i < visibles; i++) {
            Ocurrencia o = ocurrencias.get(i);
            Circle punto = new Circle(3.5);
            punto.getStyleClass().addAll("dot", o.categoria().claseCss());
            if (o.completada()) {
                punto.getStyleClass().add("done");
            }
            puntos.getChildren().add(punto);
        }
        if (ocurrencias.size() > MAX_PUNTOS) {
            Label mas = new Label("+" + (ocurrencias.size() - MAX_PUNTOS));
            mas.getStyleClass().add("dot-more");
            puntos.getChildren().add(mas);
        }

        if (ocurrencias.isEmpty()) {
            Tooltip.uninstall(this, tooltip);
        } else {
            tooltip.setText(ocurrencias.stream()
                    .map(o -> (o.prioritaria() ? "★ " : "• ") + o.titulo())
                    .collect(Collectors.joining("\n")));
            Tooltip.install(this, tooltip);
        }
    }
}
