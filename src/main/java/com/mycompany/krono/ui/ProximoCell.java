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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Fila compacta de "Próximos 15 días": fecha, marca de categoría (punto + ícono) y título.
 * Clic = ir a ese día. Los prioritarios se distinguen por su ícono de estrella.
 */
public class ProximoCell extends ListCell<Ocurrencia> {

    private static final PseudoClass COMPLETADA = PseudoClass.getPseudoClass("done");

    private final Label fecha = new Label();
    private final StackPane marcaCaja = new StackPane();
    private final Label titulo = new Label();
    private final HBox raiz;

    public ProximoCell(Consumer<Ocurrencia> alElegir) {
        fecha.getStyleClass().add("upcoming-date");
        titulo.getStyleClass().add("upcoming-title");
        titulo.setMinWidth(0);
        titulo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titulo, Priority.ALWAYS);
        marcaCaja.setMinWidth(Region.USE_PREF_SIZE);

        raiz = new HBox(10, fecha, marcaCaja, titulo);
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
        marcaCaja.getChildren().setAll(MarcaCategoria.de(o, 4, 13));
        titulo.setText(o.etiqueta() == null ? o.titulo() : "[" + o.etiqueta() + "] " + o.titulo());
        raiz.pseudoClassStateChanged(COMPLETADA, o.completada());
        setGraphic(raiz);
    }
}
