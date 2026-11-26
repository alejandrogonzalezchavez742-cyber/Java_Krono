package com.mycompany.krono.ui.form;

import com.mycompany.krono.model.CategoriaContacto;
import com.mycompany.krono.model.Contacto;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Window;

/** Alta/edición de un contacto del directorio. */
public class ContactoForm extends FormularioBase<Contacto> {

    private final Contacto original;
    private final TextField txtNombre = new TextField();
    private final TextField txtTelefono = new TextField();
    private final ComboBox<CategoriaContacto> cmbCategoria = new ComboBox<>();
    private final TextArea txtNotas;

    public ContactoForm(Window owner, Contacto c) {
        super(owner, c.getId() == null ? "Nuevo contacto" : "Editar contacto",
                "Trabajo, Casa, Amigos o Generales (servicios y contactos ocasionales).");
        this.original = c;
        txtNotas = Campos.notas(c.getNotas());

        txtNombre.setText(c.getNombre());
        txtNombre.setPromptText("Ej. Ana López");
        txtTelefono.setText(c.getTelefono());
        txtTelefono.setPromptText("Ej. 55 1234 5678");
        // Dígitos, espacios, +, -, paréntesis y "ext"
        txtTelefono.setTextFormatter(new TextFormatter<>(cambio ->
                cambio.getControlNewText().matches("[0-9+()\\-. extEXT]{0,30}") ? cambio : null));
        cmbCategoria.getItems().setAll(CategoriaContacto.values());
        cmbCategoria.setValue(c.getCategoria() == null ? CategoriaContacto.GENERALES : c.getCategoria());
        cmbCategoria.setPrefWidth(180);

        fila("Nombre", txtNombre);
        fila("Teléfono", txtTelefono);
        fila("Categoría", cmbCategoria);
        fila("Notas rápidas", txtNotas);
    }

    @Override
    protected String validar() {
        if (txtNombre.getText() == null || txtNombre.getText().isBlank()) {
            return "Escribe el nombre del contacto.";
        }
        String tel = txtTelefono.getText() == null ? "" : txtTelefono.getText();
        if (tel.replaceAll("\\D", "").length() < 3) {
            return "El teléfono debe tener al menos 3 dígitos.";
        }
        return null;
    }

    @Override
    protected Contacto construir() {
        Contacto c = new Contacto();
        c.setId(original.getId());
        c.setNombre(txtNombre.getText().trim());
        c.setTelefono(txtTelefono.getText().trim());
        c.setCategoria(cmbCategoria.getValue());
        c.setNotas(textoONulo(txtNotas.getText()));
        return c;
    }

    @Override
    protected void enfocarInicio() {
        txtNombre.requestFocus();
    }
}
