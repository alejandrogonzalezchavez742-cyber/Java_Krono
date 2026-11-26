package com.mycompany.krono.service;

import com.mycompany.krono.App;
import java.awt.Toolkit;
import javafx.application.Platform;
import javafx.scene.media.AudioClip;

/** Tono de alarma: sounds/alarma.wav con AudioClip y, si falla, el pitido del sistema. */
public final class SonidoService {

    private static AudioClip clip;

    private SonidoService() {
    }

    /** Se puede llamar desde cualquier hilo. */
    public static void reproducir() {
        Platform.runLater(() -> {
            try {
                if (clip == null) {
                    clip = new AudioClip(App.resource("sounds/alarma.wav").toExternalForm());
                }
                clip.play();
            } catch (RuntimeException | LinkageError e) {
                System.err.println("[Krono] No se pudo reproducir alarma.wav, uso el pitido del sistema: " + e);
                Toolkit.getDefaultToolkit().beep();
            }
        });
    }
}
