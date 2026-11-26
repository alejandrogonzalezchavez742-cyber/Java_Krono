package com.mycompany.krono.ui;

import com.mycompany.krono.model.CategoriaCalendario;
import com.mycompany.krono.model.Ocurrencia;
import com.mycompany.krono.util.Iconos;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

/**
 * Marca de categoría: el punto de color y, junto a él, un ícono pequeño del mismo color.
 *
 * <pre>
 *   Asunto importante  ● 📌   (prioritario: ● ⭐)
 *   Junta / reunión    ● 👥
 *   Cumpleaños         ● 🎂
 *   Corte de tarjeta   ● 💳
 *   Límite de pago     ● 💵
 * </pre>
 * Los íconos son vectoriales (no emojis): JavaFX dibuja los emojis en negro y con tamaños
 * desiguales, mientras que un trazo vectorial toma el color de la categoría y sigue el tema.
 */
public final class MarcaCategoria {

    private MarcaCategoria() {
    }

    /** Trazo del ícono según la categoría; los asuntos prioritarios llevan estrella en lugar de pin. */
    public static String trazo(CategoriaCalendario categoria, boolean prioritaria) {
        return switch (categoria) {
            case TAREA -> prioritaria ? Iconos.ESTRELLA : Iconos.PIN;
            case JUNTA -> Iconos.GRUPO;
            case CUMPLEANOS -> Iconos.PASTEL;
            case CORTE -> Iconos.TARJETA;
            case PAGO -> Iconos.BILLETE;
        };
    }

    /** Marca de una ocurrencia; si está completada se muestra atenuada. */
    public static HBox de(Ocurrencia o, double radioPunto, double tamanoIcono) {
        HBox marca = crear(o.categoria(), o.prioritaria(), radioPunto, tamanoIcono);
        if (o.completada()) {
            marca.getStyleClass().add("done");
        }
        return marca;
    }

    public static HBox crear(CategoriaCalendario categoria, boolean prioritaria, double radioPunto, double tamanoIcono) {
        Circle punto = new Circle(radioPunto);
        punto.getStyleClass().addAll("dot", categoria.claseCss());
        HBox marca = new HBox(2, punto,
                Iconos.crear(trazo(categoria, prioritaria), tamanoIcono, "cat-icon", categoria.claseCss()));
        marca.setAlignment(Pos.CENTER);
        marca.setMinWidth(HBox.USE_PREF_SIZE);
        marca.getStyleClass().add("dot-pair");
        return marca;
    }

    /** Ancho que ocupa una marca: diámetro del punto + separación + ícono. */
    public static double ancho(double radioPunto, double tamanoIcono) {
        return radioPunto * 2 + 2 + tamanoIcono;
    }
}
