package py.una.BD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Acceso a la tabla viajes. Implementa la logica de datos del Servicio 2
 * (Registrar viaje): descontar el pasaje del saldo de la tarjeta y dejar
 * registrado el viaje, todo dentro de una misma transaccion.
 */
public class ViajeDAO {

    // Estados posibles de un viaje (se guardan en viajes.estado y se devuelven al validador)
    public static final String COBRADO = "COBRADO";
    public static final String SALDO_INSUFICIENTE = "SALDO_INSUFICIENTE";
    public static final String TARJETA_INEXISTENTE = "TARJETA_INEXISTENTE";
    public static final String TARJETA_INHABILITADA = "TARJETA_INHABILITADA";

    /**
     * Intenta cobrar el pasaje y registra el viaje.
     *
     * - Si la tarjeta existe, esta HABILITADA y su saldo alcanza: descuenta
     *   el monto y el viaje queda COBRADO.
     * - Si no: no toca el saldo y el viaje queda con el motivo del rechazo
     *   (SALDO_INSUFICIENTE, TARJETA_INEXISTENTE o TARJETA_INHABILITADA).
     *
     * En ambos casos se inserta una fila en viajes y se devuelve su id.
     */
    public ResultadoCobro registrarViaje(String idTarjeta, String idVehiculo, String idLinea,
                                         String fechaHora, double monto) throws Exception {
        try (Connection conn = Bd.getConnection()) {
            // Descuento + insercion del viaje van en una sola transaccion:
            // o se hacen las dos cosas o ninguna.
            conn.setAutoCommit(false);
            try {
                String estado;
                double montoCobrado;
                double saldoRestante;

                // Descuento atomico: la condicion "saldo >= monto" se evalua dentro
                // del mismo UPDATE, que bloquea la fila. Asi dos validadores que
                // cobran a la vez la misma tarjeta no pueden dejarla en negativo
                // (no hay un "leer saldo y despues restar" con hueco en el medio).
                PreparedStatement descuento = conn.prepareStatement(
                    "UPDATE tarjetas SET saldo = saldo - ?, fecha_actualizacion = now() " +
                    "WHERE id_tarjeta = ? AND estado = 'HABILITADA' AND saldo >= ? RETURNING saldo");
                descuento.setDouble(1, monto);
                descuento.setString(2, idTarjeta);
                descuento.setDouble(3, monto);
                ResultSet rsDescuento = descuento.executeQuery();

                if (rsDescuento.next()) {
                    // El UPDATE afecto una fila: el pasaje se cobro
                    estado = COBRADO;
                    montoCobrado = monto;
                    saldoRestante = rsDescuento.getDouble("saldo");
                } else {
                    // El UPDATE no afecto ninguna fila: averiguamos por que
                    PreparedStatement consulta = conn.prepareStatement(
                        "SELECT saldo, estado FROM tarjetas WHERE id_tarjeta = ?");
                    consulta.setString(1, idTarjeta);
                    ResultSet rsTarjeta = consulta.executeQuery();

                    montoCobrado = 0.0;
                    if (!rsTarjeta.next()) {
                        estado = TARJETA_INEXISTENTE;
                        saldoRestante = 0.0;
                    } else {
                        saldoRestante = rsTarjeta.getDouble("saldo");
                        estado = TarjetaDAO.Tarjeta.HABILITADA.equalsIgnoreCase(rsTarjeta.getString("estado"))
                            ? SALDO_INSUFICIENTE
                            : TARJETA_INHABILITADA;
                    }
                }

                // Se registra el viaje (cobrado o rechazado) y se obtiene su id
                PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO viajes (id_tarjeta, id_vehiculo, id_linea, fecha_hora, monto, " +
                    "estado, monto_cobrado, saldo_restante) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "RETURNING id_viaje");
                insert.setString(1, idTarjeta);
                insert.setString(2, idVehiculo);
                insert.setString(3, idLinea);
                insert.setTimestamp(4, aTimestamp(fechaHora));
                insert.setDouble(5, monto);
                insert.setString(6, estado);
                insert.setDouble(7, montoCobrado);
                insert.setDouble(8, saldoRestante);
                ResultSet rsViaje = insert.executeQuery();
                rsViaje.next();
                int idViaje = rsViaje.getInt("id_viaje");

                conn.commit();
                return new ResultadoCobro(idViaje, estado, montoCobrado, saldoRestante);

            } catch (Exception e) {
                // Si algo fallo, se deshace el descuento para no cobrar sin registrar el viaje
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * El validador envia la fecha como LocalDateTime.toString().
     * Si viene vacia o con otro formato se usa la hora actual del servidor.
     */
    private static Timestamp aTimestamp(String fechaHora) {
        try {
            return Timestamp.valueOf(LocalDateTime.parse(fechaHora));
        } catch (Exception e) {
            return Timestamp.valueOf(LocalDateTime.now());
        }
    }

    /** Resultado del intento de cobro, con los datos que se devuelven en el S2. */
    public static class ResultadoCobro {
        public final int idViaje;
        public final String estado;
        public final double montoCobrado;
        public final double saldoRestante;

        public ResultadoCobro(int idViaje, String estado, double montoCobrado, double saldoRestante) {
            this.idViaje = idViaje;
            this.estado = estado;
            this.montoCobrado = montoCobrado;
            this.saldoRestante = saldoRestante;
        }

        public boolean fueCobrado() {
            return COBRADO.equals(estado);
        }
    }
}
