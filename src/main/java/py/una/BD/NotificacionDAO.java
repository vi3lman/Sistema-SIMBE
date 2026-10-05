package py.una.BD;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class NotificacionDAO {

    public void registrar(String idNotificacion, String idTransaccion, String estado,
                           double monto, String codigoResultado, String fechaHora) throws Exception {
        try (Connection conn = Bd.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO notificaciones_pago " +
                "(id_notificacion, id_transaccion, estado, monto, codigo_resultado, fecha_hora) " +
                "VALUES (?, ?, ?, ?, ?, ?)");
            ps.setString(1, idNotificacion);
            ps.setString(2, idTransaccion);
            ps.setString(3, estado);
            ps.setDouble(4, monto);
            ps.setString(5, codigoResultado);
            ps.setString(6, fechaHora);
            ps.executeUpdate();
        }
    }
}