package py.una.simbe.server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import py.una.BD.TarjetaDAO;
import py.una.simbe.client.SppeClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SolicitarRecargaHandler implements Runnable {

    private final Socket socketCliente;
    private final SppeClient sppeClient = new SppeClient();
    private final TarjetaDAO tarjetaDAO = new TarjetaDAO();

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

        try {
            if (resultadoPago.exito) {
                double nuevoSaldo = tarjetaDAO.acreditar(idTarjeta, resultadoPago.montoProcesado);
                respuesta.put("montoRecargado", resultadoPago.montoProcesado);
                respuesta.put("nuevoSaldo", nuevoSaldo);
                respuesta.put("estado", "APROBADO");
            } else {
                respuesta.put("montoRecargado", 0.0);
                respuesta.put("nuevoSaldo", tarjetaDAO.obtenerSaldo(idTarjeta));
                respuesta.put("estado", "ERROR_COMUNICACION_SPPE");
                respuesta.put("motivo", resultadoPago.motivo);
            }
        } catch (Exception e) {
            System.err.println("[SIMBE] Error de base de datos: " + e.getMessage());
            respuesta.put("montoRecargado", 0.0);
            respuesta.put("nuevoSaldo", 0.0);
            respuesta.put("estado", "ERROR_BASE_DATOS");
            respuesta.put("motivo", e.getMessage());
        }

        return respuesta;
    }
}