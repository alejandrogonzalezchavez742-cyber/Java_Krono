package com.mycompany.krono.controller;

import com.mycompany.krono.App;
import com.mycompany.krono.db.ConexionDB;
import com.mycompany.krono.db.ConexionDB.Diagnostico;
import com.mycompany.krono.db.ConexionDB.Estado;
import com.mycompany.krono.db.DbConfig;
import com.mycompany.krono.util.Dialogos;
import com.mycompany.krono.util.ThemeManager;
import java.io.IOException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Controlador del diálogo modal "Configurar / Enlazar Base de Datos" (view/db-config.fxml). */
public class DbConfigController {

    @FXML private Label lblMotivo;
    @FXML private TextField txtHost;
    @FXML private TextField txtPuerto;
    @FXML private TextField txtBaseDatos;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private CheckBox chkCrearTablas;
    @FXML private HBox boxEstado;
    @FXML private ProgressIndicator progreso;
    @FXML private Label lblEstado;
    @FXML private Label lblArchivo;
    @FXML private Button btnCancelar;
    @FXML private Button btnProbar;

    private Stage stage;
    private boolean listo;

    /**
     * Muestra el diálogo y espera a que se cierre.
     *
     * @return true si se guardó una conexión válida con el esquema instalado
     */
    public static boolean mostrar(Window owner, String motivo, boolean alIniciar) {
        try {
            FXMLLoader loader = new FXMLLoader(App.resource("view/db-config.fxml"));
            Parent root = loader.load();
            DbConfigController c = loader.getController();

            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            if (owner != null) {
                st.initOwner(owner);
            }
            st.setTitle(App.APP_NAME + " · Configurar base de datos");
            st.setResizable(false);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(App.resource("css/db-config.css").toExternalForm());
            ThemeManager.aplicar(scene);   // base.css + tema activo, antes de db-config.css
            st.setScene(scene);

            c.preparar(st, ConexionDB.getConfig(), motivo, alIniciar);
            st.showAndWait();
            return c.listo;
        } catch (IOException e) {
            Dialogos.error(owner, "No se pudo abrir el diálogo de configuración", e.getMessage());
            return false;
        }
    }

    private void preparar(Stage st, DbConfig cfg, String motivo, boolean alIniciar) {
        this.stage = st;

        txtHost.setText(cfg.host());
        txtPuerto.setText(String.valueOf(cfg.puerto()));
        txtBaseDatos.setText(cfg.baseDatos());
        txtUsuario.setText(cfg.usuario());
        txtPassword.setText(cfg.password());

        if (motivo != null && !motivo.isBlank()) {
            lblMotivo.setText(motivo);
            mostrarNodo(lblMotivo, true);
        }
        btnCancelar.setText(alIniciar ? "Salir de Krono" : "Cancelar");
        lblArchivo.setText("Se guardará en: " + DbConfig.archivo());

        // Solo dígitos en el puerto
        txtPuerto.textProperty().addListener((obs, viejo, nuevo) -> {
            if (!nuevo.matches("\\d{0,5}")) {
                txtPuerto.setText(viejo);
            }
        });

        // Si hay contraseña vacía conviene empezar ahí; si no, en el host
        st.setOnShown(e -> (cfg.password().isEmpty() ? txtPassword : txtHost).requestFocus());
    }

    // ================================================================ acciones

    @FXML
    private void onCancelar() {
        listo = false;
        stage.close();
    }

    @FXML
    private void onProbarYGuardar() {
        DbConfig cfg = leerFormulario();
        if (cfg == null) {
            return;
        }
        boolean crear = chkCrearTablas.isSelected();

        Task<Diagnostico> tarea = new Task<>() {
            @Override
            protected Diagnostico call() {
                return ConexionDB.diagnosticar(cfg, crear);
            }
        };
        tarea.setOnRunning(e -> ocupado(true, "Conectando a " + cfg.descripcion() + "…"));
        tarea.setOnSucceeded(e -> terminar(cfg, tarea.getValue()));
        tarea.setOnFailed(e -> {
            ocupado(false, null);
            estado("Error inesperado: " + tarea.getException().getMessage(), "estado-error");
        });

        Thread hilo = new Thread(tarea, "krono-db-test");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void terminar(DbConfig cfg, Diagnostico diag) {
        ocupado(false, null);

        if (diag.estado() == Estado.SIN_CONEXION) {
            estado(diag.mensaje(), "estado-error");
            return;
        }

        // Conectó: la configuración es válida, se guarda aunque falten tablas
        try {
            cfg.guardar();
        } catch (IOException ex) {
            estado("Conexión correcta, pero no se pudo guardar " + DbConfig.archivo() + ": " + ex.getMessage(),
                    "estado-error");
            return;
        }
        ConexionDB.setConfig(cfg);

        if (diag.lista()) {
            estado(diag.mensaje() + ". Configuración guardada.", "estado-ok");
            listo = true;
            stage.close();
        } else {
            estado(diag.mensaje() + " Marca la casilla de crear tablas o ejecuta krono_schema.sql en pgAdmin.",
                    "estado-aviso");
        }
    }

    // =============================================================== utilidades

    /** Valida los campos. Devuelve null y marca en rojo el primero inválido. */
    private DbConfig leerFormulario() {
        for (TextInputControl campo : new TextInputControl[]{txtHost, txtPuerto, txtBaseDatos, txtUsuario}) {
            campo.getStyleClass().remove("invalido");
        }
        if (falta(txtHost, "Escribe el host del servidor (por ejemplo, localhost).")
                || falta(txtPuerto, "Escribe el puerto (por defecto, 5432).")
                || falta(txtBaseDatos, "Escribe el nombre de la base de datos.")
                || falta(txtUsuario, "Escribe el usuario de PostgreSQL.")) {
            return null;
        }
        int puerto = DbConfig.parsePuerto(txtPuerto.getText(), -1);
        if (puerto < 0) {
            marcar(txtPuerto, "El puerto debe estar entre 1 y 65535.");
            return null;
        }
        return new DbConfig(txtHost.getText(), puerto, txtBaseDatos.getText(),
                txtUsuario.getText(), txtPassword.getText());
    }

    private boolean falta(TextInputControl campo, String mensaje) {
        if (campo.getText() == null || campo.getText().isBlank()) {
            marcar(campo, mensaje);
            return true;
        }
        return false;
    }

    private void marcar(TextInputControl campo, String mensaje) {
        campo.getStyleClass().add("invalido");
        campo.requestFocus();
        estado(mensaje, "estado-error");
    }

    private void ocupado(boolean si, String texto) {
        btnProbar.setDisable(si);
        btnCancelar.setDisable(si);
        progreso.setVisible(si);
        progreso.setManaged(si);
        if (texto != null) {
            estado(texto, null);
        }
    }

    private void estado(String texto, String clase) {
        lblEstado.getStyleClass().removeAll("estado-ok", "estado-aviso", "estado-error");
        if (clase != null) {
            lblEstado.getStyleClass().add(clase);
        }
        lblEstado.setText(texto);
        mostrarNodo(boxEstado, true);
        stage.sizeToScene();
    }

    private static void mostrarNodo(javafx.scene.Node n, boolean si) {
        n.setVisible(si);
        n.setManaged(si);
    }
}
