package py.una.simbe.client;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Cliente TCP interno del Modulo de Recargas de SIMBE hacia el Modulo de
 * Cobros de SPPE (Sistema 2). Invoca el Servicio 1 de SPPE ("Procesar
 * pago") para efectivizar el cobro que financia una recarga de tarjeta.
 *
 * SPPE es un repositorio/proceso aparte. Mientras ese servidor no exista
 * (o no este corriendo), la conexion fallara de forma controlada: este
 * cliente no lanza la excepcion cruda, sino que la traduce a un
 * {@link ResultadoPago} con exito = false para que el llamador arme una
 * respuesta de error prolija en vez de que SIMBE se caiga.
 */
public class SppeClient {

    private static final String SPPE_HOST = System.getProperty("sppe.host", "localhost");
    private static final int SPPE_PORT = Integer.parseInt(System.getProperty("sppe.port", "6001"));
    private static final int TIMEOUT_MS = 3000;

    public ResultadoPago procesarPago(String idTarjeta, String idTransaccion, double monto, String fechaHora) {
        JSONObject solicitud = new JSONObject();
        solicitud.put("idCliente", idTarjeta);
        solicitud.put("idMedioPago", idTransaccion);
        solicitud.put("monto", monto);
        solicitud.put("concepto", "Recarga de tarjeta " + idTarjeta);
        solicitud.put("fechaHora", fechaHora);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(SPPE_HOST, SPPE_PORT), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                System.out.println("[SIMBE->SPPE] Enviando a " + SPPE_HOST + ":" + SPPE_PORT + " -> " + solicitud.toJSONString());
                out.println(solicitud.toJSONString());

                String lineaRespuesta = in.readLine();
                if (lineaRespuesta == null) {
                    return ResultadoPago.error("SPPE cerro la conexion sin responder");
                }
                System.out.println("[SPPE->SIMBE] Respuesta -> " + lineaRespuesta);

                JSONObject respuesta = (JSONObject) new JSONParser().parse(lineaRespuesta);
                String estado = (String) respuesta.get("estado");
                double montoProcesado = ((Number) respuesta.get("montoProcesado")).doubleValue();
                boolean exito = "APROBADO".equalsIgnoreCase(estado) || "OK".equalsIgnoreCase(estado);

                return exito ? ResultadoPago.exitoso(montoProcesado) : ResultadoPago.error("SPPE rechazo el pago: " + estado);
            }

        } catch (IOException e) {
            System.err.println("[SIMBE->SPPE] No se pudo comunicar con SPPE (" + SPPE_HOST + ":" + SPPE_PORT + "): " + e.getMessage());
            return ResultadoPago.error("No se pudo comunicar con SPPE: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[SIMBE->SPPE] Respuesta invalida de SPPE: " + e.getMessage());
            return ResultadoPago.error("Respuesta invalida de SPPE: " + e.getMessage());
        }
    }

    /** Resultado simplificado de invocar el Servicio 1 de SPPE. */
    public static class ResultadoPago {
        public final boolean exito;
        public final double montoProcesado;
        public final String motivo;

        private ResultadoPago(boolean exito, double montoProcesado, String motivo) {
            this.exito = exito;
            this.montoProcesado = montoProcesado;
            this.motivo = motivo;
        }

        static ResultadoPago exitoso(double montoProcesado) {
            return new ResultadoPago(true, montoProcesado, null);
        }

        static ResultadoPago error(String motivo) {
            return new ResultadoPago(false, 0.0, motivo);
        }
    }
}
