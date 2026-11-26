package com.mycompany.krono.ui.form;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

/**
 * Fábrica y lectura de campos de formulario. Los métodos leer*() toman el texto escrito
 * aunque el usuario no haya pulsado Enter ni salido del campo.
 */
public final class Campos {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("d/M/yyyy");
    private static final Pattern HORA = Pattern.compile("^(\\d{1,2})(?:[:.h ]?(\\d{2}))?$");

    private Campos() {
    }

    // ----------------------------------------------------------------- fecha

    public static DatePicker fecha(LocalDate inicial) {
        DatePicker dp = new DatePicker(inicial);
        dp.setPromptText("dd/mm/aaaa");
        dp.setEditable(true);
        dp.setMaxWidth(Double.MAX_VALUE);
        dp.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : FECHA.format(d);
            }

            @Override
            public LocalDate fromString(String s) {
                return parsearFecha(s);
            }
        });
        return dp;
    }

    public static LocalDate leerFecha(DatePicker dp) {
        String texto = dp.getEditor().getText();
        LocalDate d = parsearFecha(texto);
        if (d != null || texto == null || texto.isBlank()) {
            dp.setValue(d);
        }
        return d;
    }

    /** Lee la fecha escrita sin modificar el campo (para vistas previas). */
    public static LocalDate verFecha(DatePicker dp) {
        LocalDate d = parsearFecha(dp.getEditor().getText());
        return d != null ? d : dp.getValue();
    }

    /** Valor actual de un spinner sin modificarlo (para vistas previas). */
    public static int verEntero(Spinner<Integer> s, int porDefecto) {
        try {
            return Integer.parseInt(s.getEditor().getText().trim());
        } catch (NumberFormatException e) {
            return s.getValue() == null ? porDefecto : s.getValue();
        }
    }

    private static LocalDate parsearFecha(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        for (DateTimeFormatter f : new DateTimeFormatter[]{FECHA, FECHA_CORTA}) {
            try {
                return LocalDate.parse(s.trim(), f);
            } catch (DateTimeParseException ignorada) {
                // probar el siguiente formato
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ hora

    /** Lista editable cada 30 minutos; también acepta lo que se escriba: 9, 930, 9:30, 09.30. */
    public static ComboBox<String> hora() {
        ComboBox<String> c = new ComboBox<>();
        c.setEditable(true);
        c.setPromptText("HH:mm");
        c.setVisibleRowCount(8);
        c.setPrefWidth(130);
        for (int h = 0; h < 24; h++) {
            c.getItems().add(String.format("%02d:00", h));
            c.getItems().add(String.format("%02d:30", h));
        }
        return c;
    }

    public static boolean horaVacia(ComboBox<String> c) {
        String t = c.getEditor().getText();
        return t == null || t.isBlank();
    }

    /** null si está vacía o no es válida (combinar con {@link #horaVacia}). */
    public static LocalTime leerHora(ComboBox<String> c) {
        String t = c.getEditor().getText();
        if (t == null) {
            return null;
        }
        Matcher m = HORA.matcher(t.trim());
        if (!m.matches()) {
            return null;
        }
        // "930" → 9 y 30; "9" → 9 y 00
        int h = Integer.parseInt(m.group(1));
        int min = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        return h < 24 && min < 60 ? LocalTime.of(h, min) : null;
    }

    public static void ponerHora(ComboBox<String> c, LocalTime h) {
        String t = h == null ? "" : String.format("%02d:%02d", h.getHour(), h.getMinute());
        c.setValue(t);
        c.getEditor().setText(t);
    }

    // ------------------------------------------------------- números y texto

    public static Spinner<Integer> entero(int min, int max, int inicial) {
        Spinner<Integer> s = new Spinner<>(min, max, Math.max(min, Math.min(max, inicial)));
        s.setEditable(true);
        s.setPrefWidth(90);
        s.getEditor().setTextFormatter(new TextFormatter<>(cambio ->
                cambio.getControlNewText().matches("\\d{0,3}") ? cambio : null));
        return s;
    }

    public static int leerEntero(Spinner<Integer> s, int min, int max) {
        int valor;
        try {
            valor = Integer.parseInt(s.getEditor().getText().trim());
        } catch (NumberFormatException e) {
            valor = s.getValue() == null ? min : s.getValue();
        }
        valor = Math.max(min, Math.min(max, valor));
        s.getValueFactory().setValue(valor);
        return valor;
    }

    public static TextArea notas(String inicial) {
        TextArea t = new TextArea(inicial);
        t.setPromptText("Opcional");
        t.setWrapText(true);
        t.setPrefRowCount(3);
        return t;
    }

    /** Acepta "1250.50", "1,250.50", "$1,250.50" o "-300". null si no es un número. */
    public static BigDecimal parsearMonto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.replace("$", "").replace(",", "").replace(" ", "").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        try {
            BigDecimal monto = new BigDecimal(limpio);
            return monto.scale() > 2 ? null : monto.setScale(2);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
