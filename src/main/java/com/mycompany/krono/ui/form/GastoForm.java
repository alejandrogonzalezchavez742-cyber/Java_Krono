package com.mycompany.krono.ui.form;

import com.mycompany.krono.model.Gasto;
import com.mycompany.krono.model.Tarjeta;
import com.mycompany.krono.service.TarjetaService;
import com.mycompany.krono.service.TarjetaService.Periodo;
import com.mycompany.krono.util.Fechas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import javafx.util.StringConverter;

/** Registro/edición de un gasto de tarjeta. Muestra en qué corte entra. */
public class GastoForm extends FormularioBase<Gasto> {

    private final Gasto original;
    private final ComboBox<Tarjeta> cmbTarjeta = new ComboBox<>();
    private final TextField txtConcepto = new TextField();
    private final TextField txtMonto = new TextField();
    private final DatePicker dpFecha;
    private final TextArea txtNotas;
    private final Label lblVistaPrevia = new Label();

    public GastoForm(Window owner, Gasto g, List<Tarjeta> tarjetas) {
        super(owner, g.getId() == null ? "Registrar gasto" : "Editar gasto",
                "Se suma al total del periodo y aparece en el aviso del día de corte.");
        this.original = g;
        dpFecha = Campos.fecha(g.getFecha() == null ? LocalDate.now() : g.getFecha());
        txtNotas = Campos.notas(g.getNotas());

        cmbTarjeta.getItems().setAll(tarjetas);
        cmbTarjeta.setMaxWidth(Double.MAX_VALUE);
        cmbTarjeta.setConverter(new StringConverter<>() {
            @Override
            public String toString(Tarjeta t) {
                return t == null ? "" : t.getNombre();
            }

            @Override
            public Tarjeta fromString(String s) {
                return null;
            }
        });
        tarjetas.stream().filter(t -> Objects.equals(t.getId(), g.getTarjetaId()))
                .findFirst().ifPresent(cmbTarjeta::setValue);

        txtConcepto.setText(g.getConcepto());
        txtConcepto.setPromptText("Ej. Supermercado");
        txtMonto.setText(g.getMonto() == null ? "" : g.getMonto().toPlainString());
        txtMonto.setPromptText("0.00");
        txtMonto.setPrefWidth(160);
        txtMonto.setMaxWidth(160);
        lblVistaPrevia.getStyleClass().add("form-preview");
        lblVistaPrevia.setMaxWidth(Double.MAX_VALUE);
        lblVistaPrevia.setWrapText(true);

        fila("Tarjeta", cmbTarjeta);
        fila("Concepto", txtConcepto);
        Label lblMonto = fila("Monto", txtMonto);
        lblMonto.setText("Monto ($)");
        fila("Fecha", dpFecha);
        fila("", lblVistaPrevia);
        fila("Notas", txtNotas);

        cmbTarjeta.valueProperty().addListener((obs, a, b) -> actualizar());
        dpFecha.valueProperty().addListener((obs, a, b) -> actualizar());
        dpFecha.getEditor().textProperty().addListener((obs, a, b) -> actualizar());
        actualizar();
    }

    private void actualizar() {
        Tarjeta t = cmbTarjeta.getValue();
        LocalDate f = Campos.verFecha(dpFecha);
        if (t == null || f == null) {
            lblVistaPrevia.setText("Usa un monto negativo para registrar una devolución.");
            return;
        }
        Periodo p = TarjetaService.periodoQueContiene(t, f);
        lblVistaPrevia.setText("Entra en el corte del " + Fechas.largo(p.corte()).toLowerCase(Fechas.ES)
                + " (pago antes del " + Fechas.diaMes(p.pago()) + ").\nUsa un monto negativo para una devolución.");
    }

    @Override
    protected String validar() {
        if (cmbTarjeta.getValue() == null) {
            return "Elige la tarjeta.";
        }
        if (txtConcepto.getText() == null || txtConcepto.getText().isBlank()) {
            return "Escribe el concepto del gasto.";
        }
        BigDecimal monto = Campos.parsearMonto(txtMonto.getText());
        if (monto == null) {
            return "Escribe un monto válido, por ejemplo 1250.50 (máximo 2 decimales).";
        }
        if (monto.signum() == 0) {
            return "El monto no puede ser cero.";
        }
        if (monto.abs().compareTo(new BigDecimal("9999999999.99")) > 0) {
            return "El monto es demasiado grande.";
        }
        if (Campos.leerFecha(dpFecha) == null) {
            return "Elige la fecha (formato dd/mm/aaaa).";
        }
        return null;
    }

    @Override
    protected Gasto construir() {
        Gasto g = new Gasto();
        g.setId(original.getId());
        g.setTarjetaId(cmbTarjeta.getValue().getId());
        g.setConcepto(txtConcepto.getText().trim());
        g.setMonto(Campos.parsearMonto(txtMonto.getText()));
        g.setFecha(Campos.leerFecha(dpFecha));
        g.setNotas(textoONulo(txtNotas.getText()));
        return g;
    }

    @Override
    protected void enfocarInicio() {
        (cmbTarjeta.getValue() == null ? cmbTarjeta : txtConcepto).requestFocus();
    }
}
