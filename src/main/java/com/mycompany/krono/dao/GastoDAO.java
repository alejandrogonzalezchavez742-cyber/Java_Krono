package com.mycompany.krono.dao;

import com.mycompany.krono.db.ConexionDB;
import com.mycompany.krono.model.Gasto;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla gastos. Un periodo de corte es (corte anterior, corte]: excluye el primero, incluye el segundo. */
public class GastoDAO {

    /** Total acumulado del periodo que termina en {@code corte}. */
    public BigDecimal totalPeriodo(long tarjetaId, LocalDate corteAnterior, LocalDate corte) throws SQLException {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM gastos WHERE tarjeta_id = ? AND fecha > ? AND fecha <= ?";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, tarjetaId);
            ps.setObject(2, corteAnterior);
            ps.setObject(3, corte);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    /** Gastos del periodo, los más recientes primero. */
    public List<Gasto> listarPeriodo(long tarjetaId, LocalDate corteAnterior, LocalDate corte) throws SQLException {
        String sql = "SELECT id, tarjeta_id, concepto, monto, fecha, notas FROM gastos "
                + "WHERE tarjeta_id = ? AND fecha > ? AND fecha <= ? ORDER BY fecha DESC, id DESC";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, tarjetaId);
            ps.setObject(2, corteAnterior);
            ps.setObject(3, corte);
            try (ResultSet rs = ps.executeQuery()) {
                List<Gasto> lista = new ArrayList<>();
                while (rs.next()) {
                    Gasto g = new Gasto();
                    g.setId(rs.getLong("id"));
                    g.setTarjetaId(rs.getLong("tarjeta_id"));
                    g.setConcepto(rs.getString("concepto"));
                    g.setMonto(rs.getBigDecimal("monto"));
                    g.setFecha(rs.getObject("fecha", LocalDate.class));
                    g.setNotas(rs.getString("notas"));
                    lista.add(g);
                }
                return lista;
            }
        }
    }

    public void insertar(Gasto g) throws SQLException {
        String sql = "INSERT INTO gastos (tarjeta_id, concepto, monto, fecha, notas) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            parametros(ps, g);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                g.setId(rs.getLong(1));
            }
        }
    }

    public void actualizar(Gasto g) throws SQLException {
        String sql = "UPDATE gastos SET tarjeta_id = ?, concepto = ?, monto = ?, fecha = ?, notas = ? WHERE id = ?";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            parametros(ps, g);
            ps.setLong(6, g.getId());
            ps.executeUpdate();
        }
    }

    public void eliminar(long id) throws SQLException {
        try (Connection cn = ConexionDB.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM gastos WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static void parametros(PreparedStatement ps, Gasto g) throws SQLException {
        ps.setLong(1, g.getTarjetaId());
        ps.setString(2, g.getConcepto());
        ps.setBigDecimal(3, g.getMonto());
        ps.setObject(4, g.getFecha());
        ps.setString(5, g.getNotas());
    }
}
