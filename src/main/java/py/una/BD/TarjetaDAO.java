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

    /**
     * Busca la tarjeta y devuelve su saldo y estado. Lo usa el Modulo de
     * Validacion (S1) para decidir si la tarjeta es valida.
     *
     * @return la tarjeta encontrada, o null si no existe
     */
    public Tarjeta obtenerTarjeta(String idTarjeta) throws Exception {
        try (Connection conn = Bd.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT saldo, estado FROM tarjetas WHERE id_tarjeta = ?");
            ps.setString(1, idTarjeta);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? new Tarjeta(idTarjeta, rs.getDouble("saldo"), rs.getString("estado")) : null;
        }
    }

    /** Datos basicos de una tarjeta tal como estan en la tabla tarjetas. */
    public static class Tarjeta {
        public static final String HABILITADA = "HABILITADA";

        public final String idTarjeta;
        public final double saldo;
        public final String estado;

        public Tarjeta(String idTarjeta, double saldo, String estado) {
            this.idTarjeta = idTarjeta;
            this.saldo = saldo;
            this.estado = estado;
        }

        /** Solo las tarjetas HABILITADAS pueden usarse para viajar. */
        public boolean estaHabilitada() {
            return HABILITADA.equalsIgnoreCase(estado);
        }
    }
}