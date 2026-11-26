package com.mycompany.krono.db;

import com.mycompany.krono.App;
import com.mycompany.krono.controller.DbConfigController;
import com.mycompany.krono.util.Dialogos;
import com.mycompany.krono.util.ThemeManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/**
 * Punto único de acceso a PostgreSQL.
 *
 * Uso desde los DAO (Fase 4):
 * <pre>
 *   try (Connection cn = ConexionDB.getConnection()) { ... }
 * </pre>
 * Krono es una aplicación de un solo usuario, así que basta con abrir una conexión
 * corta por operación y no hace falta un pool.
 */
public final class ConexionDB {

    /** Tablas que deben existir para considerar instalado el esquema. */
    public static final List<String> TABLAS =
            List.of("krono_info", "eventos", "cumpleanos", "tarjetas", "gastos", "contactos");

    private static final String SCRIPT = "sql/krono_schema.sql";
    private static final int TIMEOUT_SEGUNDOS = 5;

    public enum Estado {
        /** Conecta y el esquema está completo. */
        LISTA,
        /** Conecta, pero faltan tablas (BD vacía o incompleta). */
        SIN_TABLAS,
        /** No se pudo conectar. */
        SIN_CONEXION
    }

    /** Resultado de comprobar una configuración. */
    public record Diagnostico(Estado estado, String mensaje) {

        public boolean lista() {
            return estado == Estado.LISTA;
        }
    }

    private static volatile DbConfig config = DbConfig.cargar();

    private ConexionDB() {
    }

    // ================================================================ conexión

    public static DbConfig getConfig() {
        return config;
    }

    /** Cambia la configuración activa (no la guarda en disco: eso lo hace DbConfig.guardar()). */
    public static void setConfig(DbConfig nueva) {
        config = nueva;
    }

    /** Abre una conexión con la configuración activa. Ciérrala con try-with-resources. */
    public static Connection getConnection() throws SQLException {
        return abrir(config);
    }

    public static Connection abrir(DbConfig cfg) throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", cfg.usuario());
        props.setProperty("password", cfg.password());
        props.setProperty("connectTimeout", String.valueOf(TIMEOUT_SEGUNDOS));
        props.setProperty("loginTimeout", String.valueOf(TIMEOUT_SEGUNDOS));
        props.setProperty("ApplicationName", "Krono");
        return DriverManager.getConnection(cfg.url(), props);
    }

    // ============================================================ diagnóstico

    /**
     * Prueba la conexión y verifica el esquema. Si {@code crearSiFalta} es true y faltan
     * tablas, ejecuta el script de creación.
     *
     * Bloquea hasta {@value #TIMEOUT_SEGUNDOS} s: no lo llames en el hilo de JavaFX.
     */
    public static Diagnostico diagnosticar(DbConfig cfg, boolean crearSiFalta) {
        try (Connection cn = abrir(cfg)) {
            if (esquemaInstalado(cn)) {
                return new Diagnostico(Estado.LISTA, "Conectado a " + cfg.descripcion());
            }
            if (!crearSiFalta) {
                return new Diagnostico(Estado.SIN_TABLAS,
                        "Conectado, pero la base de datos \"" + cfg.baseDatos() + "\" no tiene las tablas de Krono.");
            }
            ejecutarScript(cn);
            return esquemaInstalado(cn)
                    ? new Diagnostico(Estado.LISTA, "Tablas creadas en " + cfg.descripcion())
                    : new Diagnostico(Estado.SIN_TABLAS, "El script se ejecutó, pero siguen faltando tablas.");
        } catch (SQLException e) {
            return new Diagnostico(Estado.SIN_CONEXION, mensajeAmigable(e, cfg));
        } catch (IOException e) {
            return new Diagnostico(Estado.SIN_TABLAS, "No se pudo leer el script SQL interno: " + e.getMessage());
        }
    }

    public static boolean esquemaInstalado(Connection cn) throws SQLException {
        String lista = String.join("','", TABLAS);   // constantes propias, sin entrada del usuario
        String sql = "SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema = current_schema() AND table_name IN ('" + lista + "')";
        try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1) == TABLAS.size();
        }
    }

    /** Ejecuta krono_schema.sql en una sola transacción: o se crea todo o nada. */
    public static void crearTablas(DbConfig cfg) throws SQLException, IOException {
        try (Connection cn = abrir(cfg)) {
            ejecutarScript(cn);
        }
    }

    private static void ejecutarScript(Connection cn) throws SQLException, IOException {
        String sql;
        try (InputStream in = App.resource(SCRIPT).openStream()) {
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        boolean autoCommit = cn.getAutoCommit();
        cn.setAutoCommit(false);
        // El driver de PostgreSQL acepta varias sentencias en un solo execute()
        // y respeta los bloques $$ ... $$ de las funciones.
        try (Statement st = cn.createStatement()) {
            st.execute(sql);
            cn.commit();
        } catch (SQLException e) {
            cn.rollback();
            throw e;
        } finally {
            cn.setAutoCommit(autoCommit);
        }
    }

    /** Traduce los códigos SQLState más comunes a un mensaje claro en español. */
    public static String mensajeAmigable(SQLException e, DbConfig cfg) {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        return switch (estado) {
            case "28P01" -> "Usuario o contraseña incorrectos para \"" + cfg.usuario() + "\".";
            case "28000" -> "El servidor rechazó al usuario \"" + cfg.usuario()
                    + "\". Revisa sus permisos (pg_hba.conf).";
            case "3D000" -> "La base de datos \"" + cfg.baseDatos() + "\" no existe en "
                    + cfg.host() + ":" + cfg.puerto() + ".";
            case "08004" -> "El servidor rechazó la conexión. Si pide contraseña, escríbela. Detalle: "
                    + e.getMessage();
            case "08001", "08006" -> "No se pudo contactar a PostgreSQL en " + cfg.host() + ":" + cfg.puerto()
                    + ". Verifica que el servicio esté en ejecución y que el host y el puerto sean correctos.";
            default -> e.getMessage() + (estado.isEmpty() ? "" : " (SQLState " + estado + ")");
        };
    }

    /** Mensaje legible para cualquier error de una operación de base de datos. */
    public static String describir(Throwable error) {
        Throwable causa = error;
        while (causa != null && !(causa instanceof SQLException)) {
            causa = causa.getCause();
        }
        if (causa instanceof SQLException sql) {
            return mensajeAmigable(sql, config);
        }
        return error == null ? "Error desconocido." : String.valueOf(error.getMessage());
    }

    // ===================================================== flujo de arranque (UI)

    /**
     * Se llama desde App.start() (hilo de JavaFX) con el diagnóstico hecho en App.init().
     *
     * @return true si Krono puede continuar; false si el usuario decidió salir.
     */
    public static boolean prepararAlInicio(Diagnostico diag) {
        return switch (diag.estado()) {
            case LISTA -> true;
            case SIN_TABLAS -> ofrecerCrearTablas(diag) || mostrarDialogo(null, diag.mensaje(), true);
            case SIN_CONEXION -> mostrarDialogo(null, diag.mensaje(), true);
        };
    }

    /**
     * Abre el diálogo modal "Configurar / Enlazar Base de Datos".
     *
     * @param owner     ventana padre (null al inicio)
     * @param motivo    mensaje de error a mostrar arriba (puede ser null)
     * @param alIniciar true: el botón secundario dice "Salir de Krono"
     * @return true si quedó una conexión válida con el esquema instalado
     */
    public static boolean mostrarDialogo(Window owner, String motivo, boolean alIniciar) {
        return DbConfigController.mostrar(owner, motivo, alIniciar);
    }

    private static boolean ofrecerCrearTablas(Diagnostico diag) {
        ButtonType crear = new ButtonType("Crear tablas", ButtonBar.ButtonData.OK_DONE);
        ButtonType configurar = new ButtonType("Configurar conexión…", ButtonBar.ButtonData.OTHER);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, null, crear, configurar);
        alerta.setTitle("Krono");
        alerta.setHeaderText("La base de datos está vacía");
        alerta.setContentText(diag.mensaje() + "\n\n¿Quieres que Krono cree las tablas ahora?");
        ThemeManager.aplicar(alerta.getDialogPane());

        if (alerta.showAndWait().orElse(configurar) != crear) {
            return false;
        }
        try {
            crearTablas(config);
            return true;
        } catch (SQLException | IOException e) {
            Dialogos.error(null, "No se pudieron crear las tablas", describir(e));
            return false;
        }
    }
}
