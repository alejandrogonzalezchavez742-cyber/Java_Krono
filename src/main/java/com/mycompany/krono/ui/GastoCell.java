package com.mycompany.krono.ui;

import com.mycompany.krono.model.Gasto;
import com.mycompany.krono.util.Fechas;
import com.mycompany.krono.util.Iconos;
import java.util.function.Consumer;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Fila de gasto: fecha, concepto/notas, monto y botones editar/eliminar. */
public class GastoCell extends ListCell<Gasto> {

    private static final PseudoClass NEGATIVO = PseudoClass.getPseudoClass("negativo");

    private final Label fecha = new Label();
    private final Label concepto = new Label();
    private final Label notas = new Label();
    private final Label monto = new Label();
    private final HBox raiz;

    public GastoCell(Consumer<Gasto> alEditar, Consumer<Gasto> alEliminar) {
        fecha.getStyleClass().add("gasto-fecha");
        concepto.getStyleClass().add("gasto-concepto");
        notas.getStyleClass().add("event-meta");
        monto.getStyleClass().add("gasto-monto");
        monto.setMinWidth(Region.USE_PREF_SIZE);

        VBox texto = new VBox(2, concepto, notas);
        texto.setMinWidth(0);
        HBox.setHgrow(texto, Priority.ALWAYS);

        Button editar = new Button();
        editar.getStyleClass().add("mini-button");
        editar.setGraphic(Iconos.crear(Iconos.EDITAR, 15));
        editar.setTooltip(new Tooltip("Editar gasto"));
        editar.setFocusTraversable(false);
        editar.setOnAction(e -> alEditar.accept(getItem()));
        Button eliminar = new Button();
        eliminar.getStyleClass().addAll("mini-button", "danger");
        eliminar.setGraphic(Iconos.crear(Iconos.ELIMINAR, 15));
        eliminar.setTooltip(new Tooltip("Eliminar gasto"));
        eliminar.setFocusTraversable(false);
        eliminar.setOnAction(e -> alEliminar.accept(getItem()));

        raiz = new HBox(12, fecha, texto, monto, new HBox(2, editar, eliminar));
        raiz.setAlignment(Pos.CENTER_LEFT);
        raiz.getStyleClass().add("gasto-item");
        setPrefWidth(0);
        setText(null);
    }

    @Override
    protected void updateItem(Gasto g, boolean vacia) {
        super.updateItem(g, vacia);
        if (vacia || g == null) {
            setGraphic(null);
            return;
        }
        fecha.setText(Fechas.corto(g.getFecha()));
        concepto.setText(g.getConcepto());
        boolean conNotas = g.getNotas() != null && !g.getNotas().isBlank();
        notas.setText(conNotas ? g.getNotas() : null);
        notas.setVisible(conNotas);
        notas.setManaged(conNotas);
        boolean devolucion = g.getMonto().signum() < 0;
        monto.setText(devolucion ? "−" + Fechas.dinero(g.getMonto().abs()) + " (devolución)" : Fechas.dinero(g.getMonto()));
        monto.pseudoClassStateChanged(NEGATIVO, devolucion);
        setGraphic(raiz);
    }
}
