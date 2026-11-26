package com.mycompany.krono.ui.form;

import com.mycompany.krono.model.ContextoJunta;
import com.mycompany.krono.model.Evento;
import com.mycompany.krono.model.TipoEvento;
import java.time.LocalDate;
import java.time.LocalTime;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Alta/edición de un asunto importante (tarea) o de una junta.
 * Una junta pide obligatoriamente: nombre, contexto (Trabajo/Casa/Amigos), fecha y hora de alarma.
 */
public class EventoForm extends FormularioBase<Evento> {

    private final Evento original;

    private final ToggleGroup grupoTipo = new ToggleGroup();
    private final ToggleButton tgTarea = new ToggleButton("Asunto / Tarea");
    private final ToggleButton tgJunta = new ToggleButton("Junta / Reunión");
    private final TextField txtTitulo = new TextField();
    private final DatePicker dpFecha;
    private final ComboBox<ContextoJunta> cmbContexto = new ComboBox<>();
    private final ComboBox<String> cmbHora = Campos.hora();
    private final CheckBox chkAlarma = new CheckBox("Avisarme con alarma a esa hora");
    private final CheckBox chkPrioritaria = new CheckBox("Prioritaria: aparece primero y destacada");
    private final CheckBox chkCompletada = new CheckBox("Completada");
    private final TextArea txtNotas;
    private final VBox opciones = new VBox(8, chkAlarma, chkPrioritaria, chkCompletada);

    private final Label lblTitulo;
    private final Label lblContexto;
    private final Label lblHora;
    private final Label lblOpciones;

    public EventoForm(Window owner, Evento e) {
        super(owner, e.getId() == null ? "Nuevo evento" : "Editar evento", "");
        this.original = e;
        dpFecha = Campos.fecha(e.getFecha());
        txtNotas = Campos.notas(e.getNotas());

        tgTarea.setToggleGroup(grupoTipo);
        tgJunta.setToggleGroup(grupoTipo);
        tgTarea.getStyleClass().add("nav-button");
        tgJunta.getStyleClass().add("nav-button");
        HBox segmento = new HBox(2, tgTarea, tgJunta);
        segmento.getStyleClass().add("nav-segment");
        segmento.setAlignment(Pos.CENTER_LEFT);
        segmento.setMaxWidth(HBox.USE_PREF_SIZE);

        cmbContexto.getItems().setAll(ContextoJunta.values());
        cmbContexto.setPromptText("Elige…");
        cmbContexto.setPrefWidth(180);

        fila("Tipo", segmento);
        lblTitulo = fila("Asunto", txtTitulo);
        fila("Fecha", dpFecha);
        lblContexto = fila("Contexto", cmbContexto);
        lblHora = fila("Hora", cmbHora);
        lblOpciones = fila("", opciones);
        fila("Notas", txtNotas);

        // Valores iniciales
        (e.getTipo() == TipoEvento.JUNTA ? tgJunta : tgTarea).setSelected(true);
        txtTitulo.setText(e.getTitulo());
        cmbContexto.setValue(e.getContexto());
        Campos.ponerHora(cmbHora, e.getHora());
        chkAlarma.setSelected(e.isAlarmaActiva());
        chkPrioritaria.setSelected(e.isPrioritaria());
        chkCompletada.setSelected(e.isCompletada());
        mostrar(chkCompletada, e.getId() != null);

        grupoTipo.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);
            } else {
                actualizarTipo();
            }
        });
        cmbHora.getEditor().textProperty().addListener((obs, a, b) -> actualizarAlarma());
        actualizarTipo();
    }

    private boolean esJunta() {
        return tgJunta.isSelected();
    }

    private void actualizarTipo() {
        boolean junta = esJunta();
        lblTitulo.setText(junta ? "Nombre de la junta" : "Asunto");
        txtTitulo.setPromptText(junta ? "Ej. Junta de equipo" : "Ej. Pagar predial");
        mostrarFila(lblContexto, cmbContexto, junta);
        lblHora.setText(junta ? "Hora de alarma" : "Hora (opcional)");
        mostrar(chkAlarma, !junta);
        mostrar(chkPrioritaria, !junta);
        // Sin casillas visibles, la fila entera desaparece (si no, queda un hueco)
        mostrarFila(lblOpciones, opciones, !junta || original.getId() != null);
        setSubtitulo(junta
                ? "La alarma mostrará el nombre y su etiqueta, por ejemplo: [Trabajo] Junta de equipo."
                : "Puedes registrar varios asuntos el mismo día. Las prioritarias se muestran primero.");
        actualizarAlarma();
        if (getDialogPane().getScene() != null) {
            getDialogPane().getScene().getWindow().sizeToScene();
        }
    }

    private void actualizarAlarma() {
        boolean sinHora = Campos.horaVacia(cmbHora);
        chkAlarma.setDisable(sinHora);
        if (sinHora) {
            chkAlarma.setSelected(false);
        }
    }

    @Override
    protected String validar() {
        if (txtTitulo.getText() == null || txtTitulo.getText().isBlank()) {
            return esJunta() ? "Escribe el nombre de la junta." : "Escribe el asunto.";
        }
        if (Campos.leerFecha(dpFecha) == null) {
            return "Elige la fecha (formato dd/mm/aaaa).";
        }
        if (!Campos.horaVacia(cmbHora) && Campos.leerHora(cmbHora) == null) {
            return "La hora debe tener formato HH:mm, por ejemplo 09:30.";
        }
        if (esJunta()) {
            if (cmbContexto.getValue() == null) {
                return "Elige el contexto de la junta: Trabajo, Casa o Amigos.";
            }
            if (Campos.horaVacia(cmbHora)) {
                return "Una junta necesita la hora de la alarma.";
            }
        }
        return null;
    }

    @Override
    protected Evento construir() {
        boolean junta = esJunta();
        LocalTime hora = Campos.leerHora(cmbHora);
        LocalDate fecha = Campos.leerFecha(dpFecha);

        Evento e = new Evento();
        e.setId(original.getId());
        e.setTipo(junta ? TipoEvento.JUNTA : TipoEvento.TAREA);
        e.setTitulo(txtTitulo.getText().trim());
        e.setFecha(fecha);
        e.setHora(hora);
        e.setContexto(junta ? cmbContexto.getValue() : null);
        e.setPrioritaria(!junta && chkPrioritaria.isSelected());
        e.setAlarmaActiva(hora != null && (junta || chkAlarma.isSelected()));
        e.setCompletada(original.getId() != null && chkCompletada.isSelected());
        e.setNotas(textoONulo(txtNotas.getText()));
        return e;
    }

    @Override
    protected void enfocarInicio() {
        txtTitulo.requestFocus();
    }
}
