package com.mycompany.krono.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;

/** Icono de Krono (cuadro azul redondeado con una "K"), dibujado a cualquier tamaño. */
public final class IconoApp {

    private static final Color ACENTO = new Color(0x02, 0x84, 0xC7);

    private IconoApp() {
    }

    /** Para la bandeja del sistema (AWT). */
    public static BufferedImage awt(int tamano) {
        BufferedImage img = new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            int radio = Math.round(tamano * 0.42f);
            g.setColor(ACENTO);
            g.fillRoundRect(0, 0, tamano, tamano, radio, radio);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, Math.round(tamano * 0.68f)));
            FontMetrics fm = g.getFontMetrics();
            int x = (tamano - fm.stringWidth("K")) / 2;
            int y = (tamano - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString("K", x, y);
        } finally {
            g.dispose();
        }
        return img;
    }

    /** Para el icono de la ventana (JavaFX). */
    public static javafx.scene.image.Image fx(int tamano) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(awt(tamano), "png", out);
            return new javafx.scene.image.Image(new ByteArrayInputStream(out.toByteArray()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
