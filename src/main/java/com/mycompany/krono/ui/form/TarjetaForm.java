package com.mycompany.krono.ui.form;

import com.mycompany.krono.model.Tarjeta;
import com.mycompany.krono.service.TarjetaService;
import com.mycompany.krono.service.TarjetaService.Periodo;
import com.mycompany.krono.util.Fechas;
import java.time.LocalDate;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Alta/edición de una tarjeta: banco/tarjeta, día de corte y fecha límite de pago
 * (por días de gracia o por día fijo del mes).
 */
public class TarjetaForm extends FormularioBase<Tarjeta> {

    private final Tarjeta original;
    private final TextField txtNombre = new TextField();
    private final Spinner<Integer> spCorte;
    private final ToggleGroup grupoPago = new ToggleGroup();
    private final RadioButton rbGracia = new RadioButton("Días de gracia:");
    private final RadioButton rbDiaFijo = new RadioButton("Día fijo del mes:");
    private final Spinner<Integer> spGracia;
    private final Spinner<Integer> spDiaPago;
    private final CheckBox chkActiva = new CheckBox("Activa (aparece en el calendario y genera avisos)");
    private final TextArea txtNotas;
    private final Label lblVistaPrevia = new Label();

    public TarjetaForm(Window owner, Tarjeta t) {
        super(owner, t.getId() == null ? "Nueva tarjeta" : "Editar tarjeta",
                "Cada mes Krono marca el corte (ámbar) y la fecha límite de pago (verde) con el total acumulado.");
        this.original = t;
        spCorte = Campos.entero(1, 31, t.getDiaCorte() == 0 ? LocalDate.now().getDayOfMonth() : t.getDiaCorte());
        spGracia = Campos.entero(1, 60, t.getDiasGracia() == null ? 20 : t.getDiasGracia());
        spDiaPago = Campos.entero(1, 31, t.getDiaLimitePago() == null ? 5 : t.getDiaLimitePago());
        txtNotas = Campos.notas(t.getNotas());

        txtNombre.setText(t.getNombre());
        txtNombre.setPromptText("Ej. BBVA Azul");
        rbGracia.setToggleGroup(grupoPago);
        rbDiaFijo.setToggleGroup(grupoPago);
        (t.getDiaLimitePago() != null ? rbDiaFijo : rbGracia).setSelected(true);
        chkActiva.setSelected(t.isActiva());

        lblVistaPrevia.getStyleClass().add("form-preview");
        lblVistaPrevia.setMaxWidth(Double.MAX_VALUE);
        lblVistaPrevia.setWrapText(true);

        HBox corte = new HBox(10, spCorte, sufijo("de cada mes"));
        corte.setAlignment(Pos.CENTER_LEFT);
        HBox gracia = new HBox(10, rbGracia, spGracia, sufijo("días después del corte"));
        gracia.setAlignment(Pos.CENTER_LEFT);
        HBox diaFijo = new HBox(10, rbDiaFijo, spDiaPago, sufijo("de cada mes"));
        diaFijo.setAlignment(Pos.CENTER_LEFT);
        rbGracia.setMinWidth(130);
        rbDiaFijo.setMinWidth(130);

        fila("Banco / tarjeta", txtNombre);
        fila("Día de corte", corte);
        fila("Límite de pago", new VBox(8, gracia, diaFijo));
        fila("", lblVistaPrevia);
        if (t.getId() != null) {
            fila("", chkActiva);
        }
        fila("Notas", txtNotas);

        grupoPago.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);
            }
            actualizar();
        });
        spCorte.valueProperty().addListener((obs, a, b) -> actualizar());
        spGracia.valueProperty().addListener((obs, a, b) -> actualizar());
        spDiaPago.valueProperty().addListener((obs, a, b) -> actualizar());
        actualizar();
    }

    private static Label sufijo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("form-hint");
        return l;
    }

    private void actualizar() {
        spGracia.setDisable(!rbGracia.isSelected());
        spDiaPago.setDisable(!rbDiaFijo.isSelected());
        Tarjeta temp = armar(Campos.verEntero(spCorte, 1), Campos.verEntero(spGracia, 20), Campos.verEntero(spDiaPago, 5));
        Periodo p = TarjetaService.periodoQueContiene(temp, LocalDate.now());
        lblVistaPrevia.setText("Periodo actual: " + Fechas.corto(p.inicio()) + " al " + Fechas.corto(p.corte())
                + "\nPróximo corte: " + Fechas.largo(p.corte())
                + "\nPagar a más tardar: " + Fechas.largo(p.pago()));
    }

    @Override
    protected String validar() {
        if (txtNombre.getText() == null || txtNombre.getText().isBlank()) {
            return "Escribe el nombre del banco o de la tarjeta.";
        }
        return null;
    }

    @Override
    protected Tarjeta construir() {
        return armar(Campos.leerEntero(spCorte, 1, 31), Campos.leerEntero(spGracia, 1, 60),
                Campos.leerEntero(spDiaPago, 1, 31));
    }

    private Tarjeta armar(int diaCorte, int diasGracia, int diaPago) {
        Tarjeta t = new Tarjeta();
        t.setId(original.getId());
        t.setNombre(txtNombre.getText() == null ? "" : txtNombre.getText().trim());
        t.setDiaCorte(Math.max(1, Math.min(31, diaCorte)));
        if (rbGracia.isSelected()) {
            t.setDiasGracia(Math.max(1, Math.min(60, diasGracia)));
        } else {
            t.setDiaLimitePago(Math.max(1, Math.min(31, diaPago)));
        }
        t.setActiva(original.getId() == null || chkActiva.isSelected());
        t.setNotas(textoONulo(txtNotas.getText()));
        return t;
    }

    @Override
    protected void enfocarInicio() {
        txtNombre.requestFocus();
    }
}
