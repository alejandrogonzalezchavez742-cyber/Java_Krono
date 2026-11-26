package com.mycompany.krono.db;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Parámetros de conexión a PostgreSQL, persistidos en db-config.properties.
 *
 * Ubicación del archivo:
 *   1. Ruta indicada con -Dkrono.config=C:\ruta\db-config.properties (opcional)
 *   2. Por defecto: %USERPROFILE%\.krono\db-config.properties
 *
 * Se usa una carpeta fija del usuario y no la carpeta de trabajo, para que Krono lea
 * el mismo archivo al ejecutarse desde NetBeans y desde el .jar.
 */
public record DbConfig(String host, int puerto, String baseDatos, String usuario, String password) {

    public static final DbConfig POR_DEFECTO =
            new DbConfig("localhost", 5432, "krono_db", "postgres", "");

    private static final String K_HOST = "db.host";
    private static final String K_PUERTO = "db.port";
    private static final String K_BD = "db.name";
    private static final String K_USUARIO = "db.user";
    private static final String K_PASSWORD = "db.password";

    public DbConfig {
        host = host == null ? "" : host.trim();
        baseDatos = baseDatos == null ? "" : baseDatos.trim();
        usuario = usuario == null ? "" : usuario.trim();
        password = password == null ? "" : password;
    }

    /** URL JDBC. Usuario y contraseña viajan aparte, en las Properties de la conexión. */
    public String url() {
        return "jdbc:postgresql://" + host + ":" + puerto + "/" + baseDatos;
    }

    /** Texto corto para mensajes: "localhost:5432/krono_db". */
    public String descripcion() {
        return host + ":" + puerto + "/" + baseDatos;
    }

    // ------------------------------------------------------------------ archivo

    public static Path archivo() {
        String personalizado = System.getProperty("krono.config");
        if (personalizado != null && !personalizado.isBlank()) {
            return Path.of(personalizado);
        }
        return Path.of(System.getProperty("user.home"), ".krono", "db-config.properties");
    }

    /**
     * Lee la configuración. Si el archivo no existe lo crea con los valores por defecto,
     * para que puedas editarlo a mano si lo prefieres. Nunca lanza excepción: ante un
     * archivo dañado devuelve los valores por defecto.
     */
    public static DbConfig cargar() {
        Path ruta = archivo();
        if (!Files.exists(ruta)) {
            try {
                POR_DEFECTO.guardar();
            } catch (IOException e) {
                System.err.println("[Krono] No se pudo crear " + ruta + ": " + e.getMessage());
            }
            return POR_DEFECTO;
        }

        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            System.err.println("[Krono] No se pudo leer " + ruta + ": " + e.getMessage());
            return POR_DEFECTO;
        }

        return new DbConfig(
                p.getProperty(K_HOST, POR_DEFECTO.host()),
                parsePuerto(p.getProperty(K_PUERTO), POR_DEFECTO.puerto()),
                p.getProperty(K_BD, POR_DEFECTO.baseDatos()),
                p.getProperty(K_USUARIO, POR_DEFECTO.usuario()),
                p.getProperty(K_PASSWORD, POR_DEFECTO.password()));
    }

    public void guardar() throws IOException {
        Path ruta = archivo();
        if (ruta.getParent() != null) {
            Files.createDirectories(ruta.getParent());
        }
        Properties p = new Properties();
        p.setProperty(K_HOST, host);
        p.setProperty(K_PUERTO, String.valueOf(puerto));
        p.setProperty(K_BD, baseDatos);
        p.setProperty(K_USUARIO, usuario);
        p.setProperty(K_PASSWORD, password);
        try (Writer w = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            p.store(w, "Krono - conexion a PostgreSQL (la contrasena se guarda en texto plano)");
        }
    }

    /** Devuelve el puerto si es válido (1-65535) o {@code porDefecto} en otro caso. */
    public static int parsePuerto(String texto, int porDefecto) {
        if (texto == null) {
            return porDefecto;
        }
        try {
            int n = Integer.parseInt(texto.trim());
            return (n >= 1 && n <= 65535) ? n : porDefecto;
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    /** No imprime la contraseña. */
    @Override
    public String toString() {
        return "DbConfig[" + usuario + "@" + descripcion() + "]";
    }
}
