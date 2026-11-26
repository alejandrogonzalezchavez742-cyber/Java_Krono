package com.mycompany.krono.ui.form;

import com.mycompany.krono.App;
import com.mycompany.krono.util.ThemeManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Base de los formularios de Krono: encabezado, rejilla etiqueta/campo, mensaje de error y
 * botones Guardar/Cancelar. Si {@link #validar()} devuelve un mensaje, el diálogo NO se cierra.
 *
 * @param <T> lo que devuelve el formulario al guardar (Evento, Tarjeta, Gasto…)
 */
public abstract class FormularioBase<T> extends Dialog<T> {

    private final GridPane rejilla = new GridPane();
    private final Label lblError = new Label();
    private final Label lblSubtitulo = new Label();
    private final ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
    private int fila;

    protected FormularioBase(Window owner, String titulo, String subtitulo) {
        setTitle(App.APP_NAME + " · " + titulo);
        if (owner != null) {
            initOwner(owner);
        }

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("form-title");
        lblSubtitulo.setText(subtitulo);
        lblSubtitulo.getStyleClass().add("form-subtitle");
        lblSubtitulo.setWrapText(true);
        lblSubtitulo.setMinHeight(Region.USE_PREF_SIZE);   // pasa a 2 líneas en vez de cortarse con "…"

        rejilla.setHgap(14);
        rejilla.setVgap(12);
        ColumnConstraints etiquetas = new ColumnConstraints();
        etiquetas.setMinWidth(130);
        ColumnConstraints campos = new ColumnConstraints();
        campos.setHgrow(Priority.ALWAYS);
        campos.setFillWidth(true);
        rejilla.getColumnConstraints().addAll(etiquetas, campos);

        lblError.getStyleClass().add("form-error");
        lblError.setWrapText(true);
        lblError.setMinHeight(Region.USE_PREF_SIZE);
        mostrar(lblError, false);

        VBox contenido = new VBox(16, new VBox(2, lblTitulo, lblSubtitulo), rejilla, lblError);

        DialogPane panel = getDialogPane();
        panel.getStyleClass().add("form-dialog");
        panel.setContent(contenido);
        panel.setPrefWidth(520);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        panel.getButtonTypes().setAll(btnGuardar, cancelar);
        ThemeManager.aplicar(panel);

        Button guardar = (Button) panel.lookupButton(btnGuardar);
        guardar.getStyleClass().add("accent-button");
        guardar.addEventFilter(ActionEvent.ACTION, e -> {
            String error = validar();
            if (error != null) {
                lblError.setText(error);
                mostrar(lblError, true);
                getDialogPane().getScene().getWindow().sizeToScene();
                e.consume();   // no cierra el diálogo
            }
        });

        setResultConverter(boton -> boton == btnGuardar ? construir() : null);
        setOnShown(e -> Platform.runLater(this::enfocarInicio));
    }

    /** Mensaje del primer problema encontrado, o null si todo es válido. */
    protected abstract String validar();

    /** Crea el objeto con los datos ya validados. */
    protected abstract T construir();

    /** Campo que recibe el foco al abrir. */
    protected abstract void enfocarInicio();

    // ----------------------------------------------------------- utilidades

    /** Agrega una fila "etiqueta: control" y devuelve la etiqueta (para cambiar su texto u ocultarla). */
    protected Label fila(String etiqueta, Node control) {
        Label l = new Label(etiqueta);
        l.getStyleClass().add("form-label");
        rejilla.add(l, 0, fila);
        rejilla.add(control, 1, fila);
        GridPane.setHgrow(control, Priority.ALWAYS);
        fila++;
        return l;
    }

    protected void setSubtitulo(String texto) {
        lblSubtitulo.setText(texto);
    }

    protected static void mostrar(Node n, boolean visible) {
        n.setVisible(visible);
        n.setManaged(visible);
    }

    /** Muestra u oculta una fila completa (etiqueta y control). */
    protected static void mostrarFila(Label etiqueta, Node control, boolean visible) {
        mostrar(etiqueta, visible);
        mostrar(control, visible);
    }

    protected static String textoONulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
