package com.mycompany.krono.ui;

import com.mycompany.krono.model.CategoriaCalendario;
import com.mycompany.krono.model.Ocurrencia;
import com.mycompany.krono.util.Fechas;
import com.mycompany.krono.util.Iconos;
import java.util.ArrayList;
import java.util.List;
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

/**
 * Tarjeta de un evento en "Detalle del día": barra de color, insignias (PRIORITARIA,
 * [Trabajo], total del corte), título, hora/notas y botones completar, editar y eliminar.
 */
public class OcurrenciaCell extends ListCell<Ocurrencia> {

    private static final PseudoClass COMPLETADA = PseudoClass.getPseudoClass("done");

    private final Region barra = new Region();
    private final HBox insignias = new HBox(4);
    private final Label titulo = new Label();
    private final Label meta = new Label();
    private final Button btnCompletar = boton("ok");
    private final Button btnEditar = boton(null);
    private final Button btnEliminar = boton("danger");
    private final HBox raiz;

    public OcurrenciaCell(Consumer<Ocurrencia> alCompletar, Consumer<Ocurrencia> alEditar,
                          Consumer<Ocurrencia> alEliminar) {
        barra.getStyleClass().add("event-bar");
        titulo.getStyleClass().add("event-title");
        titulo.setWrapText(true);
        meta.getStyleClass().add("event-meta");
        meta.setWrapText(true);
        insignias.setAlignment(Pos.CENTER_LEFT);

        VBox texto = new VBox(4, insignias, titulo, meta);
        texto.setMinWidth(0);
        HBox.setHgrow(texto, Priority.ALWAYS);

        btnEditar.setGraphic(Iconos.crear(Iconos.EDITAR, 15));
        btnEditar.setTooltip(new Tooltip("Editar"));
        btnEliminar.setGraphic(Iconos.crear(Iconos.ELIMINAR, 15));
        btnEliminar.setTooltip(new Tooltip("Eliminar"));
        HBox acciones = new HBox(2, btnCompletar, btnEditar, btnEliminar);
        acciones.setAlignment(Pos.TOP_RIGHT);
        acciones.setMinWidth(Region.USE_PREF_SIZE);

        raiz = new HBox(10, barra, texto, acciones);
        raiz.getStyleClass().add("event-item");

        btnCompletar.setOnAction(e -> alCompletar.accept(getItem()));
        btnEditar.setOnAction(e -> alEditar.accept(getItem()));
        btnEliminar.setOnAction(e -> alEliminar.accept(getItem()));

        // Ancho de la celda = ancho de la lista: el texto se ajusta en vez de desbordar
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

        barra.getStyleClass().setAll("event-bar", o.categoria().claseCss());
        titulo.setText(o.titulo());
        raiz.pseudoClassStateChanged(COMPLETADA, o.completada());

        List<Label> badges = new ArrayList<>();
        if (o.prioritaria()) {
            badges.add(insignia("★ PRIORITARIA", "badge-priority"));
        }
        if (o.etiqueta() != null) {
            badges.add(insignia("[" + o.etiqueta() + "]", "badge-junta"));
        }
        switch (o.categoria()) {
            case CUMPLEANOS -> badges.add(insignia("Cumpleaños", "badge-cumple"));
            case CORTE -> badges.add(insignia("Total " + Fechas.dinero(o.monto()), "badge-corte"));
            case PAGO -> badges.add(insignia("A pagar " + Fechas.dinero(o.monto()), "badge-pago"));
            default -> { }
        }
        insignias.getChildren().setAll(badges);
        insignias.setVisible(!badges.isEmpty());
        insignias.setManaged(!badges.isEmpty());

        String hora = o.hora() == null ? null : (o.categoria() == CategoriaCalendario.JUNTA ? "Alarma " : "") + Fechas.hora(o.hora());
        String detalle = o.detalle() == null || o.detalle().isBlank() ? null : o.detalle();
        String textoMeta = hora == null ? detalle : (detalle == null ? hora : hora + " · " + detalle);
        meta.setText(textoMeta);
        meta.setVisible(textoMeta != null);
        meta.setManaged(textoMeta != null);

        btnCompletar.setVisible(o.completable());
        btnCompletar.setManaged(o.completable());
        btnCompletar.setGraphic(Iconos.crear(o.completada() ? Iconos.DESHACER : Iconos.CHECK, 15));
        btnCompletar.setTooltip(new Tooltip(o.completada() ? "Marcar como pendiente" : "Marcar como completada"));

        setGraphic(raiz);
    }

    private static Label insignia(String texto, String clase) {
        Label l = new Label(texto);
        l.getStyleClass().addAll("badge", clase);
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    private static Button boton(String claseExtra) {
        Button b = new Button();
        b.getStyleClass().add("mini-button");
        if (claseExtra != null) {
            b.getStyleClass().add(claseExtra);
        }
        b.setFocusTraversable(false);
        return b;
    }
}
