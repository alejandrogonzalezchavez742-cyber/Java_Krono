package com.mycompany.krono.dao;

import com.mycompany.krono.db.ConexionDB;
import com.mycompany.krono.model.CategoriaContacto;
import com.mycompany.krono.model.Contacto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla contactos. */
public class ContactoDAO {

    /** Todos, en orden alfabético. El filtrado por categoría y la búsqueda se hacen en memoria. */
    public List<Contacto> listarTodos() throws SQLException {
        String sql = "SELECT id, nombre, telefono, categoria, notas FROM contactos ORDER BY lower(nombre), id";
        try (Connection cn = ConexionDB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Contacto> lista = new ArrayList<>();
            while (rs.next()) {
                Contacto c = new Contacto();
                c.setId(rs.getLong("id"));
                c.setNombre(rs.getString("nombre"));
                c.setTelefono(rs.getString("telefono"));
                c.setCategoria(CategoriaContacto.valueOf(rs.getString("categoria")));
                c.setNotas(rs.getString("notas"));
                lista.add(c);
            }
            return lista;
        }
    }

    public void insertar(Contacto c) throws SQLException {
        String sql = "INSERT INTO contactos (nombre, telefono, categoria, notas) VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            parametros(ps, c);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                c.setId(rs.getLong(1));
            }
        }
    }

    public void actualizar(Contacto c) throws SQLException {
        String sql = "UPDATE contactos SET nombre = ?, telefono = ?, categoria = ?, notas = ? WHERE id = ?";
        try (Connection cn = ConexionDB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            parametros(ps, c);
            ps.setLong(5, c.getId());
            ps.executeUpdate();
        }
    }

    public void eliminar(long id) throws SQLException {
        try (Connection cn = ConexionDB.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM contactos WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static void parametros(PreparedStatement ps, Contacto c) throws SQLException {
        ps.setString(1, c.getNombre());
        ps.setString(2, c.getTelefono());
        ps.setString(3, c.getCategoria().name());
        ps.setString(4, c.getNotas());
    }
}
