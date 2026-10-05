package py.una.simbe.server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import py.una.BD.TarjetaDAO;
import py.una.BD.ValidacionDAO;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

/**
 * Atiende un datagrama del Servicio 1 (Validar tarjeta): consulta la
 * tarjeta en la base, decide si es valida, registra la validacion y
 * responde al validador con el resultado y el saldo disponible.
 */
public class ValidarTarjetaHandler implements Runnable {

    // Motivos posibles de la respuesta
    public static final String OK = "OK";
    public static final String TARJETA_INEXISTENTE = "TARJETA_INEXISTENTE";
    public static final String TARJETA_INHABILITADA = "TARJETA_INHABILITADA";
    public static final String ERROR_BASE_DATOS = "ERROR_BASE_DATOS";

    private final DatagramSocket socket;
    private final DatagramPacket paquete;
    private final TarjetaDAO tarjetaDAO = new TarjetaDAO();
    private final ValidacionDAO validacionDAO = new ValidacionDAO();

    public ValidarTarjetaHandler(DatagramSocket socket, DatagramPacket paquete) {
        this.socket = socket;
        this.paquete = paquete;
    }

    @Override
    public void run() {
        try {
            // Solo se leen los bytes realmente recibidos, no todo el buffer
            String lineaJson = new String(paquete.getData(), 0, paquete.getLength(), StandardCharsets.UTF_8).trim();
            System.out.println("[SIMBE] Solicitud de validacion recibida: " + lineaJson);

            JSONObject solicitud = (JSONObject) new JSONParser().parse(lineaJson);
            JSONObject respuesta = validarTarjeta(solicitud);

            // La respuesta se envia a la misma direccion y puerto de origen del datagrama
            byte[] datos = respuesta.toJSONString().getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(datos, datos.length, paquete.getAddress(), paquete.getPort()));
            System.out.println("[SIMBE] Respuesta de validacion enviada: " + respuesta.toJSONString());

        } catch (Exception e) {
            System.err.println("[SIMBE] Error atendiendo solicitud de validacion: " + e.getMessage());
        }
    }

    /**
     * Una tarjeta es valida si existe y esta HABILITADA. El saldo se
     * informa siempre (0 si la tarjeta no existe) para que el validador
     * pueda mostrarlo al pasajero.
     */
    private JSONObject validarTarjeta(JSONObject solicitud) {
        String idTarjeta = (String) solicitud.get("idTarjeta");
        String idVehiculo = (String) solicitud.get("idVehiculo");
        String idLinea = (String) solicitud.get("idLinea");
        String fechaHora = (String) solicitud.get("fechaHora");

        JSONObject respuesta = new JSONObject();
        respuesta.put("idTarjeta", idTarjeta);

        try {
            TarjetaDAO.Tarjeta tarjeta = tarjetaDAO.obtenerTarjeta(idTarjeta);

            boolean tarjetaValida;
            String motivo;
            double saldoDisponible;

            if (tarjeta == null) {
                tarjetaValida = false;
                motivo = TARJETA_INEXISTENTE;
                saldoDisponible = 0.0;
            } else if (!tarjeta.estaHabilitada()) {
                tarjetaValida = false;
                motivo = TARJETA_INHABILITADA;
                saldoDisponible = tarjeta.saldo;
            } else {
                tarjetaValida = true;
                motivo = OK;
                saldoDisponible = tarjeta.saldo;
            }

            // Se deja constancia de la validacion; el id lo genera la base
            int idValidacion = validacionDAO.registrar(idTarjeta, idVehiculo, idLinea, fechaHora,
                                                       tarjetaValida, motivo, saldoDisponible);

            respuesta.put("idValidacion", idValidacion);
            respuesta.put("tarjetaValida", tarjetaValida);
            respuesta.put("motivo", motivo);
            respuesta.put("saldoDisponible", saldoDisponible);

        } catch (Exception e) {
            // Sin base de datos no se puede asegurar nada: se responde como no valida
            System.err.println("[SIMBE] Error de base de datos: " + e.getMessage());
            respuesta.put("idValidacion", null);
            respuesta.put("tarjetaValida", false);
            respuesta.put("motivo", ERROR_BASE_DATOS);
            respuesta.put("saldoDisponible", 0.0);
        }

        return respuesta;
    }
}
