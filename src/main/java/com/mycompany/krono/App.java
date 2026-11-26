package com.mycompany.krono;

import com.mycompany.krono.db.ConexionDB;
import com.mycompany.krono.service.AlarmaService;
import com.mycompany.krono.service.NotificacionService;
import com.mycompany.krono.tray.TrayManager;
import com.mycompany.krono.util.IconoApp;
import com.mycompany.krono.util.ThemeManager;
import java.io.IOException;
import java.net.URL;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de entrada JavaFX de Krono.
 *
 * Arranque: comprueba la BD (init) → enlaza o crea tablas si hace falta → ventana principal
 * con tema → icono en la bandeja del sistema → servicio de alarmas en segundo plano.
 */
public class App extends Application {

    public static final String APP_NAME = "Krono";
    private static final String BASE = "/com/mycompany/krono/";

    private static Scene scene;
    private static AlarmaService alarmas;

    /** Resultado de la prueba de conexión hecha en init(), antes de mostrar ninguna ventana. */
    private ConexionDB.Diagnostico diagnosticoInicial;

    /** Corre en el hilo "JavaFX-Launcher": aquí sí se puede bloquear esperando a PostgreSQL. */
    @Override
    public void init() {
        diagnosticoInicial = ConexionDB.diagnosticar(ConexionDB.getConfig(), false);
    }

    @Override
    public void start(Stage stage) throws IOException {
        // Sin BD no se cierra de golpe: ofrece crear tablas o abre el diálogo de enlace
        if (!ConexionDB.prepararAlInicio(diagnosticoInicial)) {
            Platform.exit();
            return;
        }

        scene = new Scene(loadFXML("primary"), 1240, 780);
        ThemeManager.aplicar(scene);
        stage.setTitle(APP_NAME);
        stage.getIcons().addAll(IconoApp.fx(16), IconoApp.fx(32), IconoApp.fx(48), IconoApp.fx(256));
        stage.setMinWidth(1000);
        stage.setMinHeight(680);
        stage.setScene(scene);

        // La X oculta Krono en la bandeja (si el sistema la tiene); "Salir" está en su menú
        if (!TrayManager.instalar(stage)) {
            stage.setOnCloseRequest(e -> salir());
        }
        stage.show();

        alarmas = new AlarmaService(new NotificacionService());
        alarmas.iniciar();
    }

    /** Cierre real de Krono (menú "Salir" de la bandeja). */
    public static void salir() {
        if (alarmas != null) {
            alarmas.detener();
        }
        TrayManager.quitar();
        Platform.exit();
        System.exit(0);
    }

    public static Scene getScene() {
        return scene;
    }

    public static Parent loadFXML(String fxml) throws IOException {
        return new FXMLLoader(resource("view/" + fxml + ".fxml")).load();
    }

    /** Resuelve un recurso dentro de /com/mycompany/krono/ (view, css, sounds, sql). */
    public static URL resource(String relativePath) {
        URL url = App.class.getResource(BASE + relativePath);
        if (url == null) {
            throw new IllegalStateException("Recurso no encontrado: " + BASE + relativePath);
        }
        return url;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
