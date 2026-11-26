package com.mycompany.krono.dao;

import com.mycompany.krono.db.ConexionDB;
import com.mycompany.krono.model.Tarjeta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla tarjetas. */
public class TarjetaDAO {

    private static final String SELECT =
            "SELECT id, nombre, dia_corte, dias_gracia, dia_limite_pago, activa, notas FROM tarjetas ";

    public List<Tarjeta> listarActivas() throws SQLException {
        return listar(SELECT + "WHERE activa ORDER BY lower(nombre)");
    }

    /** Activas primero. */
    public List<Tarjeta> listarTodas() throws SQLException {
        return listar(SELECT + "ORDER BY activa DESC, lower(nombre)");
    }

    public void insertar(Tarjeta t) throws SQLException {
        String sql = "INSERT INTO tarjetas (nombre, dia_corte, dias_gracia, dia_limite_pago, activa, notas) "
                + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            parametros(ps, t);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                t.setId(rs.getLong(1));
            }
        }
    }

    public void actualizar(Tarjeta t) throws SQLException {
        String sql = "UPDATE tarjetas SET nombre = ?, dia_corte = ?, dias_gracia = ?, dia_limite_pago = ?, "
                + "activa = ?, notas = ? WHERE id = ?";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            parametros(ps, t);
            ps.setLong(7, t.getId());
            ps.executeUpdate();
        }
    }

    /** Elimina la tarjeta y, en cascada, todos sus gastos. */
    public void eliminar(long id) throws SQLException {
        try (Connection cn = ConexionDB.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM tarjetas WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** true solo la primera vez que se pide avisar de ese corte. */
    public boolean reclamarAvisoCorte(long id, LocalDate corte) throws SQLException {
        return reclamar("ultimo_aviso_corte", id, corte);
    }

    /** true solo la primera vez que se pide avisar de esa fecha de pago. */
    public boolean reclamarAvisoPago(long id, LocalDate pago) throws SQLException {
        return reclamar("ultimo_aviso_pago", id, pago);
    }

    // ------------------------------------------------------------------ apoyo

    private boolean reclamar(String columna, long id, LocalDate fecha) throws SQLException {
        // columna: constante interna, nunca viene del usuario
        String sql = "UPDATE tarjetas SET " + columna + " = ? WHERE id = ? AND " + columna + " IS DISTINCT FROM ?";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, fecha);
            ps.setLong(2, id);
            ps.setObject(3, fecha);
            return ps.executeUpdate() == 1;
        }
    }

    private static void parametros(PreparedStatement ps, Tarjeta t) throws SQLException {
        ps.setString(1, t.getNombre());
        ps.setInt(2, t.getDiaCorte());
        setEntero(ps, 3, t.getDiasGracia());
        setEntero(ps, 4, t.getDiaLimitePago());
        ps.setBoolean(5, t.isActiva());
        ps.setString(6, t.getNotas());
    }

    private static void setEntero(PreparedStatement ps, int i, Integer valor) throws SQLException {
        if (valor == null) {
            ps.setNull(i, Types.SMALLINT);
        } else {
            ps.setInt(i, valor);
        }
    }

    private static List<Tarjeta> listar(String sql) throws SQLException {
        try (Connection cn = ConexionDB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Tarjeta> lista = new ArrayList<>();
            while (rs.next()) {
                Tarjeta t = new Tarjeta();
                t.setId(rs.getLong("id"));
                t.setNombre(rs.getString("nombre"));
                t.setDiaCorte(rs.getInt("dia_corte"));
                t.setDiasGracia(rs.getObject("dias_gracia", Integer.class));
                t.setDiaLimitePago(rs.getObject("dia_limite_pago", Integer.class));
                t.setActiva(rs.getBoolean("activa"));
                t.setNotas(rs.getString("notas"));
                lista.add(t);
            }
            return lista;
        }
    }
}
