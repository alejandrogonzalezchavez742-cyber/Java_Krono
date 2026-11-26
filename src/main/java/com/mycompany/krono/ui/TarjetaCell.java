package com.mycompany.krono.ui;

import com.mycompany.krono.model.Tarjeta;
import com.mycompany.krono.service.TarjetaService.ResumenTarjeta;
import com.mycompany.krono.util.Fechas;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Tarjeta en la lista: nombre, configuración de corte/pago y total del periodo actual. */
public class TarjetaCell extends ListCell<ResumenTarjeta> {

    private static final PseudoClass INACTIVA = PseudoClass.getPseudoClass("inactiva");

    private final Label nombre = new Label();
    private final Label config = new Label();
    private final Label total = new Label();
    private final Label corte = new Label();
    private final VBox raiz;

    public TarjetaCell() {
        nombre.getStyleClass().add("tarjeta-nombre");
        config.getStyleClass().add("tarjeta-config");
        total.getStyleClass().addAll("badge", "badge-corte");
        total.setMinWidth(Region.USE_PREF_SIZE);
        corte.getStyleClass().add("tarjeta-config");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox pie = new HBox(8, total, espacio, corte);
        pie.setAlignment(Pos.CENTER_LEFT);

        raiz = new VBox(4, nombre, config, pie);
        raiz.getStyleClass().add("tarjeta-item");
        setPrefWidth(0);
        setText(null);
    }

    @Override
    protected void updateItem(ResumenTarjeta r, boolean vacia) {
        super.updateItem(r, vacia);
        if (vacia || r == null) {
            setGraphic(null);
            return;
        }
        Tarjeta t = r.tarjeta();
        nombre.setText(t.getNombre() + (t.isActiva() ? "" : "  · inactiva"));
        config.setText(describir(t));
        total.setText(Fechas.dinero(r.total()));
        corte.setText("Corte " + Fechas.corto(r.periodo().corte()));
        raiz.pseudoClassStateChanged(INACTIVA, !t.isActiva());
        setGraphic(raiz);
    }

    /** "Corte día 15 · pago 20 días después" */
    public static String describir(Tarjeta t) {
        String pago = t.getDiasGracia() != null
                ? "pago " + t.getDiasGracia() + " días después"
                : "pago el día " + t.getDiaLimitePago();
        return "Corte día " + t.getDiaCorte() + " · " + pago;
    }
}
