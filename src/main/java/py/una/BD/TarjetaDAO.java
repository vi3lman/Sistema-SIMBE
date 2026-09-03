package py.una.BD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TarjetaDAO {

    public double obtenerSaldo(String idTarjeta) throws Exception {
        try (Connection conn = Bd.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT saldo FROM tarjetas WHERE id_tarjeta = ?");
            ps.setString(1, idTarjeta);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble("saldo") : 0.0;
        }
    }

    /**
     * Acredita el monto de forma atomica (UPDATE ... RETURNING) y devuelve
     * el saldo ya actualizado. Si la tarjeta no existe, la crea (para que
     * el flujo no se caiga en la demo).
     */
    public double acreditar(String idTarjeta, double monto) throws Exception {
        try (Connection conn = Bd.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE tarjetas SET saldo = saldo + ?, fecha_actualizacion = now() " +
                "WHERE id_tarjeta = ? RETURNING saldo");
            ps.setDouble(1, monto);
            ps.setString(2, idTarjeta);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("saldo");
            }

            // La tarjeta no existia: la creamos con el monto acreditado
            PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO tarjetas (id_tarjeta, saldo) VALUES (?, ?)");
            insert.setString(1, idTarjeta);
            insert.setDouble(2, monto);
            insert.executeUpdate();
            return monto;
        }
    }
}