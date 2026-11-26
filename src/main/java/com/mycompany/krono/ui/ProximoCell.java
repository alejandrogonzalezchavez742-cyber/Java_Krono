package com.mycompany.krono.ui;

import com.mycompany.krono.model.Ocurrencia;
import com.mycompany.krono.util.Fechas;
import java.util.function.Consumer;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Circle;

/** Fila compacta de "Próximos 15 días": fecha, punto de color y título. Clic = ir a ese día. */
public class ProximoCell extends ListCell<Ocurrencia> {

    private static final PseudoClass COMPLETADA = PseudoClass.getPseudoClass("done");

    private final Label fecha = new Label();
    private final Circle punto = new Circle(4);
    private final Label titulo = new Label();
    private final Label prioridad = new Label("★");
    private final HBox raiz;

    public ProximoCell(Consumer<Ocurrencia> alElegir) {
        fecha.getStyleClass().add("upcoming-date");
        titulo.getStyleClass().add("upcoming-title");
        titulo.setMinWidth(0);
        HBox.setHgrow(titulo, Priority.ALWAYS);
        titulo.setMaxWidth(Double.MAX_VALUE);
        prioridad.getStyleClass().add("upcoming-star");

        raiz = new HBox(10, fecha, punto, titulo, prioridad);
        raiz.setAlignment(Pos.CENTER_LEFT);
        raiz.getStyleClass().add("upcoming-item");
        raiz.setOnMouseClicked(e -> {
            if (getItem() != null) {
                alElegir.accept(getItem());
            }
        });

        setPrefWidth(0);
        setText(null);
    }

    @Override
    protected void updateItem(Ocurrencia o, boolean vacia) {
        super.updateItem(o, vacia);
        if (vacia || o == null) {
            setGraphic(null);
            return;
        }
        fecha.setText(Fechas.corto(o.fecha()));
        punto.getStyleClass().setAll("dot", o.categoria().claseCss());
        titulo.setText(o.etiqueta() == null ? o.titulo() : "[" + o.etiqueta() + "] " + o.titulo());
        prioridad.setVisible(o.prioritaria());
        prioridad.setManaged(o.prioritaria());
        raiz.pseudoClassStateChanged(COMPLETADA, o.completada());
        setGraphic(raiz);
    }
}
