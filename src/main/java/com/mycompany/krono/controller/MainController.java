package com.mycompany.krono.controller;

import com.mycompany.krono.db.ConexionDB;
import com.mycompany.krono.util.Cambios;
import com.mycompany.krono.util.Iconos;
import com.mycompany.krono.util.ThemeManager;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;

/** Ventana principal: navegación entre vistas, tema claro/oscuro y configuración de la BD. */
public class MainController {

    @FXML private ToggleGroup grupoVistas;
    @FXML private ToggleButton tabCalendario;
    @FXML private ToggleButton tabTarjetas;
    @FXML private ToggleButton tabDirectorio;
    @FXML private Button btnTema;
    @FXML private Button btnConfigBD;

    /** Raíz de calendario.fxml y su controlador (inyectados por fx:include fx:id="calendario"). */
    @FXML private Node calendario;
    @FXML private DirectorioController directorioController;

    @FXML private Node tarjetas;
    @FXML private Node directorio;

    private Map<Toggle, Node> vistas;

    @FXML
    private void initialize() {
        vistas = Map.of(tabCalendario, calendario, tabTarjetas, tarjetas, tabDirectorio, directorio);
        vistas.forEach((toggle, vista) -> vista.setVisible(toggle.isSelected()));

        grupoVistas.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);   // siempre hay una vista elegida
                return;
            }
            vistas.forEach((toggle, vista) -> vista.setVisible(toggle == nuevo));
        });

        btnConfigBD.setGraphic(Iconos.crear(Iconos.BASE_DATOS, 18));
        btnConfigBD.setTooltip(new Tooltip("Configurar base de datos"));
        actualizarBotonTema();
        ThemeManager.temaProperty().addListener((obs, a, b) -> actualizarBotonTema());
    }

    @FXML
    private void onAlternarTema() {
        ThemeManager.alternar();
    }

    @FXML
    private void onConfigurarBD() {
        if (ConexionDB.mostrarDialogo(btnConfigBD.getScene().getWindow(), null, false)) {
            Cambios.notificar();          // calendario y tarjetas
            directorioController.recargar();
        }
    }

    /** En modo claro muestra la luna (pasar a oscuro) y viceversa. */
    private void actualizarBotonTema() {
        boolean oscuro = ThemeManager.esOscuro();
        btnTema.setGraphic(Iconos.crear(oscuro ? Iconos.SOL : Iconos.LUNA, 18));
        btnTema.setTooltip(new Tooltip(oscuro ? "Cambiar a modo claro" : "Cambiar a modo oscuro"));
    }
}
