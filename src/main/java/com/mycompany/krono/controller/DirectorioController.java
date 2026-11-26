package com.mycompany.krono.controller;

import com.mycompany.krono.dao.ContactoDAO;
import com.mycompany.krono.db.ConexionDB;
import com.mycompany.krono.model.CategoriaContacto;
import com.mycompany.krono.model.Contacto;
import com.mycompany.krono.ui.ContactoCell;
import com.mycompany.krono.ui.form.ContactoForm;
import com.mycompany.krono.util.Dialogos;
import com.mycompany.krono.util.Tareas;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

/**
 * Vista Directorio: contactos en 4 categorías (Trabajo, Casa, Amigos, Generales) con filtros
 * rápidos, búsqueda sin distinguir acentos y copia del número con un clic.
 */
public class DirectorioController {

    @FXML private Label lblConteo;
    @FXML private TextField txtBuscar;
    @FXML private HBox boxFiltros;
    @FXML private ListView<Contacto> lstContactos;

    private final ContactoDAO dao = new ContactoDAO();
    private final ToggleGroup grupo = new ToggleGroup();
    /** null = "Todos". */
    private final Map<ToggleButton, CategoriaContacto> filtros = new LinkedHashMap<>();
    private List<Contacto> todos = List.of();

    @FXML
    private void initialize() {
        agregarFiltro("Todos", null).setSelected(true);
        for (CategoriaContacto c : CategoriaContacto.values()) {
            agregarFiltro(c.etiqueta(), c);
        }
        grupo.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);
            } else {
                aplicarFiltro();
            }
        });

        txtBuscar.textProperty().addListener((obs, a, b) -> aplicarFiltro());
        lstContactos.setCellFactory(lv -> new ContactoCell(this::editar, this::eliminar));
        lstContactos.setFocusTraversable(false);

        recargar();
    }

    private ToggleButton agregarFiltro(String texto, CategoriaContacto categoria) {
        ToggleButton b = new ToggleButton(texto);
        b.getStyleClass().add("nav-button");
        b.setToggleGroup(grupo);
        filtros.put(b, categoria);
        boxFiltros.getChildren().add(b);
        return b;
    }

    // ================================================================== datos

    public void recargar() {
        Tareas.ejecutar(dao::listarTodos, lista -> {
            todos = lista;
            aplicarFiltro();
        }, error -> Dialogos.error(ventana(), "No se pudieron cargar los contactos", ConexionDB.describir(error)));
    }

    private void aplicarFiltro() {
        CategoriaContacto categoria = categoriaElegida();
        String buscado = normalizar(txtBuscar.getText());

        List<Contacto> visibles = new ArrayList<>();
        for (Contacto c : todos) {
            boolean enCategoria = categoria == null || c.getCategoria() == categoria;
            if (enCategoria && coincide(c, buscado)) {
                visibles.add(c);
            }
        }
        lstContactos.getItems().setAll(visibles);

        // Contador en cada filtro: "Trabajo 3"
        filtros.forEach((boton, cat) -> {
            long n = cat == null ? todos.size() : todos.stream().filter(c -> c.getCategoria() == cat).count();
            boton.setText((cat == null ? "Todos" : cat.etiqueta()) + "  " + n);
        });
        lblConteo.setText(todos.size() + (todos.size() == 1 ? " contacto" : " contactos"));

        Label vacio = new Label(todos.isEmpty()
                ? "Aún no tienes contactos.\nUsa «Nuevo contacto» para agregar el primero."
                : "Ningún contacto coincide con el filtro.");
        vacio.getStyleClass().add("empty-text");
        lstContactos.setPlaceholder(vacio);
    }

    /** Busca en nombre, notas y en el teléfono (también solo por dígitos: "5512" encuentra "55 12 …"). */
    private static boolean coincide(Contacto c, String buscado) {
        if (buscado.isEmpty()) {
            return true;
        }
        String digitos = buscado.replaceAll("\\D", "");
        return normalizar(c.getNombre()).contains(buscado)
                || normalizar(c.getNotas()).contains(buscado)
                || normalizar(c.getTelefono()).contains(buscado)
                || (!digitos.isEmpty() && c.getTelefono().replaceAll("\\D", "").contains(digitos));
    }

    /** Minúsculas y sin acentos: "Lucía" se encuentra escribiendo "lucia". */
    private static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }

    private CategoriaContacto categoriaElegida() {
        return filtros.get((ToggleButton) grupo.getSelectedToggle());
    }

    // ================================================================ acciones

    @FXML
    private void onNuevo() {
        Contacto c = new Contacto();
        CategoriaContacto cat = categoriaElegida();
        c.setCategoria(cat == null ? CategoriaContacto.GENERALES : cat);
        new ContactoForm(ventana(), c).showAndWait().ifPresent(n -> guardar(() -> dao.insertar(n)));
    }

    private void editar(Contacto c) {
        new ContactoForm(ventana(), c).showAndWait().ifPresent(n -> guardar(() -> dao.actualizar(n)));
    }

    private void eliminar(Contacto c) {
        if (Dialogos.confirmar(ventana(), "¿Eliminar a " + c.getNombre() + "?",
                "Se eliminará el contacto y su teléfono " + c.getTelefono() + ".", "Eliminar")) {
            guardar(() -> dao.eliminar(c.getId()));
        }
    }

    private void guardar(Acciones.OperacionBD operacion) {
        Tareas.ejecutar(() -> {
            operacion.ejecutar();
            return null;
        }, r -> recargar(),
                error -> Dialogos.error(ventana(), "No se pudieron guardar los cambios", ConexionDB.describir(error)));
    }

    private Window ventana() {
        return lstContactos.getScene() == null ? null : lstContactos.getScene().getWindow();
    }
}
