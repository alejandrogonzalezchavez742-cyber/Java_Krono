package com.mycompany.krono.util;

import com.mycompany.krono.App;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

/**
 * Tema claro/oscuro intercambiable en caliente.
 *
 * Cada escena carga base.css (componentes, usando colores con nombre como -k-bg o -k-text)
 * más light-theme.css o dark-theme.css, que solo definen esos colores. Cambiar de tema es
 * reemplazar una hoja de estilos: no hay que reconstruir la interfaz.
 */
public final class ThemeManager {

    public enum Tema {
        CLARO("css/light-theme.css"),
        OSCURO("css/dark-theme.css");

        private final String hoja;

        Tema(String hoja) {
            this.hoja = hoja;
        }
    }

    private static final String BASE = "css/base.css";
    private static final String PREF = "tema";

    private static final ObjectProperty<Tema> TEMA = new SimpleObjectProperty<>(leerPreferencia());
    /** Escenas abiertas. Débil: una ventana cerrada no queda retenida en memoria. */
    private static final Set<Scene> ESCENAS = Collections.newSetFromMap(new WeakHashMap<>());

    static {
        TEMA.addListener((obs, viejo, nuevo) -> {
            ESCENAS.forEach(s -> reaplicar(s.getStylesheets()));
            Preferencias.set(PREF, nuevo.name());
        });
    }

    private ThemeManager() {
    }

    public static ReadOnlyObjectProperty<Tema> temaProperty() {
        return TEMA;
    }

    public static Tema getTema() {
        return TEMA.get();
    }

    public static boolean esOscuro() {
        return TEMA.get() == Tema.OSCURO;
    }

    public static void alternar() {
        TEMA.set(esOscuro() ? Tema.CLARO : Tema.OSCURO);
    }

    /** Aplica el tema a una escena y la mantiene sincronizada con los cambios futuros. */
    public static void aplicar(Scene escena) {
        ESCENAS.add(escena);
        reaplicar(escena.getStylesheets());
    }

    /** Para Alert/Dialog: se aplica sobre el DialogPane. */
    public static void aplicar(DialogPane panel) {
        reaplicar(panel.getStylesheets());
    }

    /** Pone base.css y el tema al principio, conservando hojas propias (p. ej. db-config.css). */
    private static void reaplicar(ObservableList<String> hojas) {
        List<String> nuestras = List.of(url(BASE), url(Tema.CLARO.hoja), url(Tema.OSCURO.hoja));
        hojas.removeAll(nuestras);
        hojas.addAll(0, List.of(url(BASE), url(TEMA.get().hoja)));
    }

    private static String url(String ruta) {
        return App.resource(ruta).toExternalForm();
    }

    private static Tema leerPreferencia() {
        try {
            return Tema.valueOf(Preferencias.get(PREF, Tema.CLARO.name()));
        } catch (IllegalArgumentException e) {
            return Tema.CLARO;
        }
    }
}
