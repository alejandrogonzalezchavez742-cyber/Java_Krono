package com.mycompany.krono.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

/** Formatos de fecha, hora y dinero en español, con los textos exactos de la interfaz. */
public final class Fechas {

    public static final Locale ES = Locale.forLanguageTag("es-MX");

    private static final String[] MESES = {
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };
    /** Índice = DayOfWeek.getValue() - 1 (lunes = 0). */
    private static final String[] DIAS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};
    private static final String[] DIAS_ABREV = {"Lun.", "Mar.", "Mié.", "Jue.", "Vie.", "Sáb.", "Dom."};
    private static final String[] DIAS_CORTOS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Fechas() {
    }

    /** Día del mes ajustado a la duración del mes: día 31 en abril = 30 de abril. */
    public static LocalDate ajustarDia(YearMonth mes, int dia) {
        return mes.atDay(Math.max(1, Math.min(dia, mes.lengthOfMonth())));
    }

    public static String mes(Month m) {
        return MESES[m.getValue() - 1];
    }

    /** "Mié." */
    public static String diaAbrev(LocalDate f) {
        return DIAS_ABREV[f.getDayOfWeek().getValue() - 1];
    }

    public static String diaSemana(DayOfWeek d) {
        return DIAS[d.getValue() - 1];
    }

    /** Número de semana ISO 8601 (la que usa Windows/Outlook por defecto en México). */
    public static int semana(LocalDate f) {
        return f.get(WeekFields.ISO.weekOfWeekBasedYear());
    }

    /** "Miércoles, 25 de noviembre" */
    public static String largo(LocalDate f) {
        String dia = diaSemana(f.getDayOfWeek());
        return Character.toUpperCase(dia.charAt(0)) + dia.substring(1)
                + ", " + f.getDayOfMonth() + " de " + mes(f.getMonth()).toLowerCase(ES);
    }

    /** "Jue 26 nov" */
    public static String corto(LocalDate f) {
        return DIAS_CORTOS[f.getDayOfWeek().getValue() - 1] + " " + f.getDayOfMonth() + " "
                + mes(f.getMonth()).substring(0, 3).toLowerCase(ES);
    }

    /** "25/11" */
    public static String diaMes(LocalDate f) {
        return String.format("%02d/%02d", f.getDayOfMonth(), f.getMonthValue());
    }

    public static String hora(LocalTime h) {
        return h == null ? "" : HORA.format(h);
    }

    /** "$1,234.50" */
    public static String dinero(BigDecimal monto) {
        return NumberFormat.getCurrencyInstance(ES).format(monto == null ? BigDecimal.ZERO : monto);
    }
}
