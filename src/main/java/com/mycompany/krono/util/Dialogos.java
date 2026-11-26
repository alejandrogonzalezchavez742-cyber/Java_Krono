package com.mycompany.krono.util;

import com.mycompany.krono.App;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Alertas con el tema activo aplicado. */
public final class Dialogos {

    private Dialogos() {
    }

    public static void info(Window owner, String encabezado, String mensaje) {
        crear(Alert.AlertType.INFORMATION, owner, encabezado, mensaje).showAndWait();
    }

    public static void error(Window owner, String encabezado, String mensaje) {
        crear(Alert.AlertType.ERROR, owner, encabezado, mensaje).showAndWait();
    }

    /** Pide confirmación. El botón de aceptar lleva el texto de la acción ("Eliminar"). */
    public static boolean confirmar(Window owner, String encabezado, String mensaje, String textoAceptar) {
        Alert a = crear(Alert.AlertType.CONFIRMATION, owner, encabezado, mensaje);
        ButtonType aceptar = new ButtonType(textoAceptar, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(aceptar, cancelar);
        return a.showAndWait().orElse(cancelar) == aceptar;
    }

    public static Alert crear(Alert.AlertType tipo, Window owner, String encabezado, String mensaje) {
        Alert a = new Alert(tipo);
        a.setTitle(App.APP_NAME);
        a.setHeaderText(encabezado);
        a.setContentText(mensaje);
        if (owner != null) {
            a.initOwner(owner);
        }
        ThemeManager.aplicar(a.getDialogPane());
        return a;
    }
}
