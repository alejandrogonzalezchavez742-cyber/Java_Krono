package com.mycompany.krono.tray;

import com.mycompany.krono.App;
import com.mycompany.krono.util.IconoApp;
import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.lang.reflect.InvocationTargetException;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Icono de Krono en la bandeja del sistema de Windows.
 *
 * Al cerrar la ventana con la X, Krono no se cierra: se oculta y sigue en la bandeja para
 * que las alarmas funcionen. Clic derecho en el icono: "Abrir Krono" o "Salir".
 * Doble clic en el icono: abre la ventana.
 *
 * AWT y JavaFX tienen hilos distintos: lo que toca el icono va al hilo de AWT
 * (EventQueue) y lo que toca la ventana vuelve al de JavaFX (Platform.runLater).
 */
public final class TrayManager {

    private static volatile TrayIcon icono;
    private static boolean avisoDeBandejaMostrado;

    private TrayManager() {
    }

    /**
     * Instala el icono y hace que la X oculte la ventana en lugar de cerrar Krono.
     *
     * @return false si este sistema no tiene bandeja (entonces la X cierra Krono normalmente)
     */
    public static boolean instalar(Stage ventana) {
        Toolkit.getDefaultToolkit();   // inicia AWT antes de consultar la bandeja
        if (!SystemTray.isSupported()) {
            return false;
        }
        try {
            EventQueue.invokeAndWait(() -> crearIcono(ventana));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (InvocationTargetException e) {
            System.err.println("[Krono] No se pudo crear el icono de la bandeja: " + e.getCause());
            return false;
        }
        if (icono == null) {
            return false;
        }

        Platform.setImplicitExit(false);
        ventana.setOnCloseRequest(e -> {
            e.consume();
            ventana.hide();
            if (!avisoDeBandejaMostrado) {
                avisoDeBandejaMostrado = true;
                notificar("Krono sigue activo",
                        "Krono sigue en la bandeja para avisarte de tus alarmas. "
                                + "Clic derecho en el icono > Salir para cerrarlo.");
            }
        });
        return true;
    }

    public static boolean disponible() {
        return icono != null;
    }

    /** Notificación emergente nativa de Windows. Se puede llamar desde cualquier hilo. */
    public static void notificar(String titulo, String mensaje) {
        TrayIcon i = icono;
        if (i != null) {
            EventQueue.invokeLater(() -> i.displayMessage(titulo, mensaje, TrayIcon.MessageType.INFO));
        }
    }

    /** Quita el icono (si no, queda un "fantasma" en la bandeja hasta pasar el ratón por encima). */
    public static void quitar() {
        TrayIcon i = icono;
        icono = null;
        if (i == null) {
            return;
        }
        try {
            EventQueue.invokeAndWait(() -> SystemTray.getSystemTray().remove(i));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            System.err.println("[Krono] No se pudo quitar el icono de la bandeja: " + e.getCause());
        }
    }

    // ------------------------------------------------------------ hilo AWT

    private static void crearIcono(Stage ventana) {
        MenuItem abrir = new MenuItem("Abrir Krono");
        abrir.addActionListener(e -> Platform.runLater(() -> mostrarVentana(ventana)));
        MenuItem salir = new MenuItem("Salir");
        salir.addActionListener(e -> Platform.runLater(App::salir));

        PopupMenu menu = new PopupMenu();
        menu.add(abrir);
        menu.addSeparator();
        menu.add(salir);

        TrayIcon nuevo = new TrayIcon(IconoApp.awt(64), App.APP_NAME, menu);
        nuevo.setImageAutoSize(true);
        nuevo.addActionListener(e -> Platform.runLater(() -> mostrarVentana(ventana)));  // doble clic
        try {
            SystemTray.getSystemTray().add(nuevo);
            icono = nuevo;
        } catch (AWTException e) {
            System.err.println("[Krono] La bandeja del sistema rechazó el icono: " + e.getMessage());
        }
    }

    // --------------------------------------------------------- hilo JavaFX

    private static void mostrarVentana(Stage ventana) {
        ventana.show();
        if (ventana.isIconified()) {
            ventana.setIconified(false);
        }
        ventana.toFront();
        ventana.requestFocus();
    }
}
