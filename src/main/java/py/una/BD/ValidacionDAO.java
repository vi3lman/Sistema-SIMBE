package py.una.BD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Acceso a la tabla validaciones. Guarda cada consulta del Servicio 1
 * (Validar tarjeta) para tener trazabilidad y generar el idValidacion.
 */
public class ValidacionDAO {

    /**
     * Inserta una validacion y devuelve el id generado por la base
     * (columna SERIAL, obtenida con RETURNING).
     */
    public int registrar(String idTarjeta, String idVehiculo, String idLinea, String fechaHora,
                         boolean tarjetaValida, String motivo, double saldoDisponible) throws Exception {
        try (Connection conn = Bd.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO validaciones (id_tarjeta, id_vehiculo, id_linea, fecha_hora, " +
                "tarjeta_valida, motivo, saldo_disponible) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING id_validacion");
            ps.setString(1, idTarjeta);
            ps.setString(2, idVehiculo);
            ps.setString(3, idLinea);
            ps.setTimestamp(4, aTimestamp(fechaHora));
            ps.setBoolean(5, tarjetaValida);
            ps.setString(6, motivo);
            ps.setDouble(7, saldoDisponible);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("id_validacion");
        }
    }

    /**
     * El validador envia la fecha como LocalDateTime.toString()
     * (ej: 2026-10-04T10:15:30.123). Si viene vacia o con otro formato
     * se usa la hora actual del servidor para no perder el registro.
     */
    private static Timestamp aTimestamp(String fechaHora) {
        try {
            return Timestamp.valueOf(LocalDateTime.parse(fechaHora));
        } catch (Exception e) {
            return Timestamp.valueOf(LocalDateTime.now());
        }
    }
}
