package py.una.simbe.server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import py.una.BD.ViajeDAO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Atiende una conexion del Servicio 2 (Registrar viaje): verifica si la
 * tarjeta tiene saldo suficiente, descuenta el pasaje si corresponde y
 * notifica el resultado al validador en el campo "mensaje".
 */
public class RegistrarViajeHandler implements Runnable {

    public static final String ERROR_BASE_DATOS = "ERROR_BASE_DATOS";

    private final Socket socketCliente;
    private final ViajeDAO viajeDAO = new ViajeDAO();

    public RegistrarViajeHandler(Socket socketCliente) {
        this.socketCliente = socketCliente;
    }

    @Override
    public void run() {
        try (Socket socket = socketCliente;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String lineaJson = in.readLine();
            System.out.println("[SIMBE] Solicitud de registro de viaje recibida: " + lineaJson);

            JSONObject solicitud = (JSONObject) new JSONParser().parse(lineaJson);
            JSONObject respuesta = registrarViaje(solicitud);

            out.println(respuesta.toJSONString());
            System.out.println("[SIMBE] Respuesta de registro de viaje enviada: " + respuesta.toJSONString());

        } catch (Exception e) {
            System.err.println("[SIMBE] Error atendiendo registro de viaje: " + e.getMessage());
        }
    }

    private JSONObject registrarViaje(JSONObject solicitud) {
        String idTarjeta = (String) solicitud.get("idTarjeta");
        String idVehiculo = (String) solicitud.get("idVehiculo");
        String idLinea = (String) solicitud.get("idLinea");
        String fechaHora = (String) solicitud.get("fechaHora");
        double monto = ((Number) solicitud.get("monto")).doubleValue();

        JSONObject respuesta = new JSONObject();
        respuesta.put("idTarjeta", idTarjeta);

        try {
            // El DAO decide en una transaccion si se cobra o se rechaza
            ViajeDAO.ResultadoCobro resultado = viajeDAO.registrarViaje(idTarjeta, idVehiculo, idLinea, fechaHora, monto);

            respuesta.put("idViaje", resultado.idViaje);
            respuesta.put("estado", resultado.estado);
            respuesta.put("montoCobrado", resultado.montoCobrado);
            respuesta.put("saldoRestante", resultado.saldoRestante);
            respuesta.put("mensaje", armarMensaje(idTarjeta, monto, resultado));

        } catch (Exception e) {
            System.err.println("[SIMBE] Error de base de datos: " + e.getMessage());
            respuesta.put("idViaje", null);
            respuesta.put("estado", ERROR_BASE_DATOS);
            respuesta.put("montoCobrado", 0.0);
            respuesta.put("saldoRestante", 0.0);
            respuesta.put("mensaje", "No se pudo registrar el viaje: " + e.getMessage());
        }

        // Notificacion del resultado: tambien queda en la consola del servidor
        System.out.println("[SIMBE][NOTIFICACION] " + respuesta.get("mensaje"));
        return respuesta;
    }

    /** Arma el texto de notificacion que se devuelve al validador segun el estado. */
    private String armarMensaje(String idTarjeta, double monto, ViajeDAO.ResultadoCobro resultado) {
        switch (resultado.estado) {
            case ViajeDAO.COBRADO:
                return "Se descontaron " + gs(resultado.montoCobrado) + " de la tarjeta " + idTarjeta
                    + ". Saldo restante: " + gs(resultado.saldoRestante);
            case ViajeDAO.SALDO_INSUFICIENTE:
                return "Saldo insuficiente: saldo " + gs(resultado.saldoRestante) + ", pasaje " + gs(monto)
                    + ". No se desconto saldo";
            case ViajeDAO.TARJETA_INHABILITADA:
                return "La tarjeta " + idTarjeta + " no esta habilitada. No se desconto saldo";
            case ViajeDAO.TARJETA_INEXISTENTE:
                return "La tarjeta " + idTarjeta + " no existe. No se desconto saldo";
            default:
                return "Estado desconocido: " + resultado.estado;
        }
    }

    /** Formatea un monto en guaranies sin decimales (ej: 2300 Gs.). */
    private static String gs(double monto) {
        return String.format("%.0f Gs.", monto);
    }
}
