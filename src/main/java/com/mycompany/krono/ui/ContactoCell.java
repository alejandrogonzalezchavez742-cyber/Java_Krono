package com.mycompany.krono.ui;

import com.mycompany.krono.model.Contacto;
import com.mycompany.krono.util.Iconos;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Contacto: avatar con iniciales, nombre, categoría, teléfono, notas y botón de copiar con un clic. */
public class ContactoCell extends ListCell<Contacto> {

    private final StackPane avatar = new StackPane();
    private final Label iniciales = new Label();
    private final Label nombre = new Label();
    private final Label categoria = new Label();
    private final Label telefono = new Label();
    private final Label notas = new Label();
    private final Button btnCopiar = new Button("Copiar");
    private final PauseTransition restaurar = new PauseTransition(Duration.seconds(1.6));
    private final HBox raiz;

    public ContactoCell(Consumer<Contacto> alEditar, Consumer<Contacto> alEliminar) {
        iniciales.getStyleClass().add("avatar-text");
        avatar.getChildren().add(iniciales);
        avatar.getStyleClass().add("avatar");

        nombre.getStyleClass().add("contacto-nombre");
        categoria.getStyleClass().add("badge");
        categoria.setMinWidth(Region.USE_PREF_SIZE);
        HBox linea1 = new HBox(8, nombre, categoria);
        linea1.setAlignment(Pos.CENTER_LEFT);
        telefono.getStyleClass().add("contacto-telefono");
        telefono.setGraphic(Iconos.crear(Iconos.TELEFONO, 14));
        telefono.setGraphicTextGap(6);
        notas.getStyleClass().add("event-meta");
        notas.setWrapText(true);

        VBox texto = new VBox(3, linea1, telefono, notas);
        texto.setMinWidth(0);
        HBox.setHgrow(texto, Priority.ALWAYS);

        btnCopiar.getStyleClass().add("copy-button");
        btnCopiar.setGraphic(Iconos.crear(Iconos.COPIAR, 14));
        btnCopiar.setTooltip(new Tooltip("Copiar el número al portapapeles"));
        btnCopiar.setMinWidth(Region.USE_PREF_SIZE);
        btnCopiar.setOnAction(e -> copiar());
        restaurar.setOnFinished(e -> {
            btnCopiar.setText("Copiar");
            btnCopiar.getStyleClass().remove("copiado");
        });

        Button editar = mini(Iconos.EDITAR, "Editar", null);
        editar.setOnAction(e -> alEditar.accept(getItem()));
        Button eliminar = mini(Iconos.ELIMINAR, "Eliminar", "danger");
        eliminar.setOnAction(e -> alEliminar.accept(getItem()));

        HBox acciones = new HBox(4, btnCopiar, editar, eliminar);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        acciones.setMinWidth(Region.USE_PREF_SIZE);

        raiz = new HBox(14, avatar, texto, acciones);
        raiz.setAlignment(Pos.CENTER_LEFT);
        raiz.getStyleClass().add("contacto-item");
        setPrefWidth(0);
        setText(null);
    }

    @Override
    protected void updateItem(Contacto c, boolean vacia) {
        super.updateItem(c, vacia);
        restaurar.stop();
        btnCopiar.setText("Copiar");
        btnCopiar.getStyleClass().remove("copiado");
        if (vacia || c == null) {
            setGraphic(null);
            return;
        }
        String css = c.getCategoria().claseCss();
        iniciales.setText(c.iniciales());
        avatar.getStyleClass().setAll("avatar", css);
        nombre.setText(c.getNombre());
        categoria.setText(c.getCategoria().etiqueta());
        categoria.getStyleClass().setAll("label", "badge", "chip", css);
        telefono.setText(c.getTelefono());
        boolean conNotas = c.getNotas() != null && !c.getNotas().isBlank();
        notas.setText(conNotas ? c.getNotas() : null);
        notas.setVisible(conNotas);
        notas.setManaged(conNotas);
        setGraphic(raiz);
    }

    private void copiar() {
        Contacto c = getItem();
        if (c == null) {
            return;
        }
        ClipboardContent contenido = new ClipboardContent();
        contenido.putString(c.getTelefono());
        Clipboard.getSystemClipboard().setContent(contenido);
        btnCopiar.setText("¡Copiado!");
        if (!btnCopiar.getStyleClass().contains("copiado")) {
            btnCopiar.getStyleClass().add("copiado");
        }
        restaurar.playFromStart();
    }

    private static Button mini(String icono, String ayuda, String clase) {
        Button b = new Button();
        b.getStyleClass().add("mini-button");
        if (clase != null) {
            b.getStyleClass().add(clase);
        }
        b.setGraphic(Iconos.crear(icono, 15));
        b.setTooltip(new Tooltip(ayuda));
        b.setFocusTraversable(false);
        return b;
    }
}
