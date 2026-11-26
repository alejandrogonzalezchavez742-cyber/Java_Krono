package com.mycompany.krono.ui.form;

import com.mycompany.krono.model.Cumpleanos;
import com.mycompany.krono.util.Fechas;
import java.time.LocalDate;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Window;

/** Alta/edición de un cumpleaños (recurrencia anual con cálculo de edad). */
public class CumpleanosForm extends FormularioBase<Cumpleanos> {

    /** Año bisiesto usado cuando no se conoce el año: así cabe el 29 de febrero. */
    private static final int ANIO_DESCONOCIDO = 2000;

    private final Cumpleanos original;
    private final TextField txtNombre = new TextField();
    private final DatePicker dpNacimiento;
    private final CheckBox chkSinAnio = new CheckBox("No sé el año de nacimiento (no se mostrará la edad)");
    private final TextArea txtNotas;
    private final Label lblVistaPrevia = new Label();

    public CumpleanosForm(Window owner, Cumpleanos c) {
        super(owner, c.getId() == null ? "Nuevo cumpleaños" : "Editar cumpleaños",
                "Se repite cada año. Krono calcula la edad y te avisa ese día.");
        this.original = c;
        dpNacimiento = Campos.fecha(c.getFechaNacimiento());
        txtNotas = Campos.notas(c.getNotas());

        txtNombre.setText(c.getNombre());
        txtNombre.setPromptText("Ej. Ana López");
        chkSinAnio.setSelected(!c.isAnioConocido());
        lblVistaPrevia.getStyleClass().add("form-preview");
        lblVistaPrevia.setMaxWidth(Double.MAX_VALUE);
        lblVistaPrevia.setWrapText(true);

        fila("Nombre", txtNombre);
        fila("Fecha de nacimiento", dpNacimiento);
        fila("", chkSinAnio);
        fila("", lblVistaPrevia);
        fila("Notas", txtNotas);

        dpNacimiento.valueProperty().addListener((obs, a, b) -> actualizarVistaPrevia());
        dpNacimiento.getEditor().textProperty().addListener((obs, a, b) -> actualizarVistaPrevia());
        chkSinAnio.selectedProperty().addListener((obs, a, b) -> actualizarVistaPrevia());
        actualizarVistaPrevia();
    }

    private void actualizarVistaPrevia() {
        LocalDate nac = Campos.verFecha(dpNacimiento);
        if (nac == null) {
            lblVistaPrevia.setText("Elige la fecha para ver el próximo cumpleaños.");
            return;
        }
        Cumpleanos temp = construirCon(nac);
        LocalDate hoy = LocalDate.now();
        LocalDate proximo = temp.ocurrenciaEn(hoy.getYear());
        if (proximo == null || proximo.isBefore(hoy)) {
            proximo = temp.ocurrenciaEn(hoy.getYear() + 1);
        }
        if (proximo == null) {
            lblVistaPrevia.setText("La fecha de nacimiento está en el futuro.");
            return;
        }
        Integer edad = temp.edadEn(proximo.getYear());
        String cuando = proximo.equals(hoy) ? "¡Hoy!" : Fechas.largo(proximo);
        lblVistaPrevia.setText("Próximo cumpleaños: " + cuando
                + (edad == null ? "" : " · cumplirá " + edad + (edad == 1 ? " año" : " años")));
    }

    @Override
    protected String validar() {
        if (txtNombre.getText() == null || txtNombre.getText().isBlank()) {
            return "Escribe el nombre de la persona.";
        }
        LocalDate nac = Campos.leerFecha(dpNacimiento);
        if (nac == null) {
            return "Elige la fecha de nacimiento (formato dd/mm/aaaa).";
        }
        if (!chkSinAnio.isSelected() && nac.isAfter(LocalDate.now())) {
            return "La fecha de nacimiento no puede ser futura.";
        }
        return null;
    }

    @Override
    protected Cumpleanos construir() {
        return construirCon(Campos.leerFecha(dpNacimiento));
    }

    private Cumpleanos construirCon(LocalDate nac) {
        boolean sinAnio = chkSinAnio.isSelected();
        Cumpleanos c = new Cumpleanos();
        c.setId(original.getId());
        c.setNombre(txtNombre.getText() == null ? "" : txtNombre.getText().trim());
        c.setFechaNacimiento(sinAnio ? LocalDate.of(ANIO_DESCONOCIDO, nac.getMonth(), nac.getDayOfMonth()) : nac);
        c.setAnioConocido(!sinAnio);
        c.setNotas(textoONulo(txtNotas.getText()));
        return c;
    }

    @Override
    protected void enfocarInicio() {
        txtNombre.requestFocus();
    }
}
