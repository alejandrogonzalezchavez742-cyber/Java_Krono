package com.mycompany.krono.dao;

import com.mycompany.krono.db.ConexionDB;
import com.mycompany.krono.model.ContextoJunta;
import com.mycompany.krono.model.Evento;
import com.mycompany.krono.model.TipoEvento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla eventos (tareas y juntas). */
public class EventoDAO {

    private static final String COLUMNAS =
            "id, tipo, titulo, notas, fecha, hora, contexto, prioritaria, completada, alarma_activa";

    /** Todos los eventos entre dos fechas (ambas incluidas), sin límite por día. */
    public List<Evento> listarEntre(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM eventos WHERE fecha BETWEEN ? AND ? ORDER BY fecha, id";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, desde);
            ps.setObject(2, hasta);
            return leer(ps);
        }
    }

    public long insertar(Evento e) throws SQLException {
        String sql = "INSERT INTO eventos (tipo, titulo, notas, fecha, hora, contexto, prioritaria, "
                + "alarma_activa, completada, completada_en) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CASE WHEN ? THEN now() END) RETURNING id";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = parametros(ps, e);
            ps.setBoolean(i++, e.isCompletada());
            ps.setBoolean(i, e.isCompletada());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long id = rs.getLong(1);
                e.setId(id);
                return id;
            }
        }
    }

    /**
     * Actualiza el evento. Si cambian la fecha o la hora, la alarma vuelve a quedar pendiente
     * (en el SET, las columnas sin asignar aún valen lo que tenían antes del UPDATE).
     */
    public void actualizar(Evento e) throws SQLException {
        String sql = "UPDATE eventos SET tipo = ?, titulo = ?, notas = ?, fecha = ?, hora = ?, contexto = ?, "
                + "prioritaria = ?, alarma_activa = ?, completada = ?, "
                + "completada_en = CASE WHEN ? THEN COALESCE(completada_en, now()) ELSE NULL END, "
                + "alarma_notificada_en = CASE WHEN fecha IS DISTINCT FROM ? OR hora IS DISTINCT FROM ? "
                + "THEN NULL ELSE alarma_notificada_en END "
                + "WHERE id = ?";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = parametros(ps, e);
            ps.setBoolean(i++, e.isCompletada());
            ps.setBoolean(i++, e.isCompletada());
            ps.setObject(i++, e.getFecha());
            setHora(ps, i++, e.getHora());
            ps.setLong(i, e.getId());
            ps.executeUpdate();
        }
    }

    public void marcarCompletada(long id, boolean completada) throws SQLException {
        String sql = "UPDATE eventos SET completada = ?, "
                + "completada_en = CASE WHEN ? THEN now() ELSE NULL END WHERE id = ?";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setBoolean(1, completada);
            ps.setBoolean(2, completada);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public void eliminar(long id) throws SQLException {
        try (Connection cn = ConexionDB.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM eventos WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Devuelve las alarmas cuya hora ya llegó y las marca como notificadas en la misma sentencia,
     * para que ninguna suene dos veces. Las vencidas antes de {@code desde} (Krono estuvo apagado
     * varios días) se marcan sin avisar.
     */
    public List<Evento> tomarAlarmasVencidas(LocalDateTime ahora, LocalDate desde) throws SQLException {
        String filtro = "alarma_activa AND alarma_notificada_en IS NULL AND NOT completada AND hora IS NOT NULL";
        try (Connection cn = ConexionDB.getConnection()) {
            try (PreparedStatement viejas = cn.prepareStatement(
                    "UPDATE eventos SET alarma_notificada_en = now() WHERE " + filtro + " AND fecha < ?")) {
                viejas.setObject(1, desde);
                viejas.executeUpdate();
            }
            String sql = "UPDATE eventos SET alarma_notificada_en = now() WHERE " + filtro
                    + " AND (fecha + hora) <= ? RETURNING " + COLUMNAS;
            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setObject(1, ahora);
                return leer(ps);
            }
        }
    }

    // ------------------------------------------------------------------ apoyo

    /** Parámetros comunes de INSERT/UPDATE (posiciones 1-8). Devuelve la siguiente posición libre. */
    private static int parametros(PreparedStatement ps, Evento e) throws SQLException {
        ps.setString(1, e.getTipo().name());
        ps.setString(2, e.getTitulo());
        ps.setString(3, e.getNotas());
        ps.setObject(4, e.getFecha());
        setHora(ps, 5, e.getHora());
        ps.setString(6, e.getContexto() == null ? null : e.getContexto().name());
        ps.setBoolean(7, e.isPrioritaria());
        ps.setBoolean(8, e.isAlarmaActiva());
        return 9;
    }

    private static void setHora(PreparedStatement ps, int i, LocalTime hora) throws SQLException {
        if (hora == null) {
            ps.setNull(i, Types.TIME);
        } else {
            ps.setObject(i, hora);
        }
    }

    private static List<Evento> leer(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Evento> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        }
    }

    static Evento mapear(ResultSet rs) throws SQLException {
        Evento e = new Evento();
        e.setId(rs.getLong("id"));
        e.setTipo(TipoEvento.valueOf(rs.getString("tipo")));
        e.setTitulo(rs.getString("titulo"));
        e.setNotas(rs.getString("notas"));
        e.setFecha(rs.getObject("fecha", LocalDate.class));
        e.setHora(rs.getObject("hora", LocalTime.class));
        String contexto = rs.getString("contexto");
        e.setContexto(contexto == null ? null : ContextoJunta.valueOf(contexto));
        e.setPrioritaria(rs.getBoolean("prioritaria"));
        e.setCompletada(rs.getBoolean("completada"));
        e.setAlarmaActiva(rs.getBoolean("alarma_activa"));
        return e;
    }
}
