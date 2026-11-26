package com.mycompany.krono.dao;

import com.mycompany.krono.db.ConexionDB;
import com.mycompany.krono.model.Cumpleanos;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla cumpleanos. */
public class CumpleanosDAO {

    public List<Cumpleanos> listarTodos() throws SQLException {
        String sql = "SELECT id, nombre, fecha_nacimiento, anio_conocido, notas FROM cumpleanos ORDER BY nombre";
        try (Connection cn = ConexionDB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Cumpleanos> lista = new ArrayList<>();
            while (rs.next()) {
                Cumpleanos c = new Cumpleanos();
                c.setId(rs.getLong("id"));
                c.setNombre(rs.getString("nombre"));
                c.setFechaNacimiento(rs.getObject("fecha_nacimiento", LocalDate.class));
                c.setAnioConocido(rs.getBoolean("anio_conocido"));
                c.setNotas(rs.getString("notas"));
                lista.add(c);
            }
            return lista;
        }
    }

    public void insertar(Cumpleanos c) throws SQLException {
        String sql = "INSERT INTO cumpleanos (nombre, fecha_nacimiento, anio_conocido, notas) "
                + "VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setObject(2, c.getFechaNacimiento());
            ps.setBoolean(3, c.isAnioConocido());
            ps.setString(4, c.getNotas());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                c.setId(rs.getLong(1));
            }
        }
    }

    /** Si cambia la fecha, el aviso de este año vuelve a quedar pendiente. */
    public void actualizar(Cumpleanos c) throws SQLException {
        String sql = "UPDATE cumpleanos SET nombre = ?, fecha_nacimiento = ?, anio_conocido = ?, notas = ?, "
                + "ultimo_aviso_anio = CASE WHEN fecha_nacimiento IS DISTINCT FROM ? THEN NULL "
                + "ELSE ultimo_aviso_anio END WHERE id = ?";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setObject(2, c.getFechaNacimiento());
            ps.setBoolean(3, c.isAnioConocido());
            ps.setString(4, c.getNotas());
            ps.setObject(5, c.getFechaNacimiento());
            ps.setLong(6, c.getId());
            ps.executeUpdate();
        }
    }

    public void eliminar(long id) throws SQLException {
        try (Connection cn = ConexionDB.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM cumpleanos WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Marca el aviso del año. Devuelve true solo la primera vez: así se avisa una sola vez por año. */
    public boolean reclamarAviso(long id, int anio) throws SQLException {
        String sql = "UPDATE cumpleanos SET ultimo_aviso_anio = ? "
                + "WHERE id = ? AND (ultimo_aviso_anio IS NULL OR ultimo_aviso_anio < ?)";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, anio);
            ps.setLong(2, id);
            ps.setInt(3, anio);
            return ps.executeUpdate() == 1;
        }
    }
}
