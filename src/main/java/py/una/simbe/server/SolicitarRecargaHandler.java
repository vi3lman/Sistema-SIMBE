package py.una.simbe.server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import py.una.simbe.client.SppeClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Atiende una solicitud del Servicio 5: Solicitar recarga de tarjeta.
 *
 * Entrada (JSON): idTarjeta, monto, idTransaccion, fechaHora
 * Salida (JSON): idTarjeta, montoRecargado, nuevoSaldo, estado
 *
 * Para poder acreditar el saldo, el Modulo de Recargas primero debe
 * efectivizar el cobro llamando por TCP al Servicio 1 de SPPE ("Procesar
 * pago") a traves de {@link SppeClient}. Esta es la comunicacion real
 * entre Sistema 1 (SIMBE) y Sistema 2 (SPPE).
 */
public class SolicitarRecargaHandler implements Runnable {

    private final Socket socketCliente;
    private final SppeClient sppeClient = new SppeClient();

    public SolicitarRecargaHandler(Socket socketCliente) {
        this.socketCliente = socketCliente;
    }

    @Override
    public void run() {
        try (Socket socket = socketCliente;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String lineaJson = in.readLine();
            System.out.println("[SIMBE] Solicitud de recarga recibida: " + lineaJson);

            JSONObject solicitud = (JSONObject) new JSONParser().parse(lineaJson);
            JSONObject respuesta = solicitarRecarga(solicitud);

            out.println(respuesta.toJSONString());
            System.out.println("[SIMBE] Respuesta de recarga enviada: " + respuesta.toJSONString());

        } catch (Exception e) {
            System.err.println("[SIMBE] Error atendiendo solicitud de recarga: " + e.getMessage());
        }
    }

    private JSONObject solicitarRecarga(JSONObject solicitud) {
        String idTarjeta = (String) solicitud.get("idTarjeta");
        String idTransaccion = (String) solicitud.get("idTransaccion");
        String fechaHora = (String) solicitud.get("fechaHora");
        double monto = ((Number) solicitud.get("monto")).doubleValue();

        SppeClient.ResultadoPago resultadoPago = sppeClient.procesarPago(idTarjeta, idTransaccion, monto, fechaHora);

        JSONObject respuesta = new JSONObject();
        respuesta.put("idTarjeta", idTarjeta);

        if (resultadoPago.exito) {
            double nuevoSaldo = SaldoTarjetas.acreditar(idTarjeta, resultadoPago.montoProcesado);
            respuesta.put("montoRecargado", resultadoPago.montoProcesado);
            respuesta.put("nuevoSaldo", nuevoSaldo);
            respuesta.put("estado", "APROBADO");
        } else {
            respuesta.put("montoRecargado", 0.0);
            respuesta.put("nuevoSaldo", SaldoTarjetas.obtenerSaldo(idTarjeta));
            respuesta.put("estado", "ERROR_COMUNICACION_SPPE");
            respuesta.put("motivo", resultadoPago.motivo);
        }

        return respuesta;
    }
}
