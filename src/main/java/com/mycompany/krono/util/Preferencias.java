package com.mycompany.krono.util;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Preferencias de la interfaz (tema, etc.) en %USERPROFILE%\.krono\krono.properties,
 * junto a db-config.properties. Se guardan al momento de cambiarlas.
 */
public final class Preferencias {

    private static final Path ARCHIVO = Path.of(System.getProperty("user.home"), ".krono", "krono.properties");
    private static Properties props;

    private Preferencias() {
    }

    public static synchronized String get(String clave, String porDefecto) {
        return cargar().getProperty(clave, porDefecto);
    }

    public static synchronized void set(String clave, String valor) {
        cargar().setProperty(clave, valor);
        try {
            Files.createDirectories(ARCHIVO.getParent());
            try (Writer w = Files.newBufferedWriter(ARCHIVO, StandardCharsets.UTF_8)) {
                props.store(w, "Krono - preferencias");
            }
        } catch (IOException e) {
            System.err.println("[Krono] No se pudieron guardar las preferencias: " + e.getMessage());
        }
    }

    private static Properties cargar() {
        if (props == null) {
            props = new Properties();
            if (Files.exists(ARCHIVO)) {
                try (Reader r = Files.newBufferedReader(ARCHIVO, StandardCharsets.UTF_8)) {
                    props.load(r);
                } catch (IOException e) {
                    System.err.println("[Krono] No se pudieron leer las preferencias: " + e.getMessage());
                }
            }
        }
        return props;
    }
}
