package com.mycompany.krono.service;

import com.mycompany.krono.tray.TrayManager;
import com.mycompany.krono.util.Dialogos;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * Muestra los avisos: notificación nativa de Windows (TrayIcon.displayMessage) más el tono.
 * Sin bandeja del sistema, recurre a una alerta de Krono.
 */
public class NotificacionService {

    /** Un aviso listo para mostrar. */
    public record Aviso(String titulo, String mensaje) {
    }

    /** Con más avisos que este número a la vez, se agrupan en una sola notificación. */
    private static final int MAX_INDIVIDUALES = 3;
    private static final long PAUSA_ENTRE_AVISOS_MS = 1500;

    /** Se llama desde el hilo de alarmas: puede esperar entre un aviso y otro. */
    public void notificar(List<Aviso> avisos) {
        if (avisos.isEmpty()) {
            return;
        }
        SonidoService.reproducir();

        List<Aviso> aMostrar = avisos.size() <= MAX_INDIVIDUALES ? avisos
                : List.of(new Aviso("Krono · " + avisos.size() + " avisos",
                        avisos.stream().map(Aviso::titulo).collect(Collectors.joining("\n"))));

        for (int i = 0; i < aMostrar.size(); i++) {
            Aviso a = aMostrar.get(i);
            if (TrayManager.disponible()) {
                TrayManager.notificar(a.titulo(), a.mensaje());
            } else {
                Platform.runLater(() -> Dialogos.crear(Alert.AlertType.INFORMATION, null, a.titulo(), a.mensaje()).show());
            }
            if (i < aMostrar.size() - 1) {
                dormir();
            }
        }
    }

    private static void dormir() {
        try {
            Thread.sleep(PAUSA_ENTRE_AVISOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
