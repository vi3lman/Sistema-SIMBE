package py.una.simbe.client;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import py.una.simbe.server.SimbeServidorCobros;
import py.una.simbe.server.SimbeServidorValidacion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Scanner;

/**
 * Simula el Dispositivo Validador instalado en el vehiculo. Cuando un
 * pasajero apoya la tarjeta:
 *   1. Valida la tarjeta con el Servicio 1 del SIMBE por UDP.
 *   2. Si es valida, registra el viaje (cobro del pasaje) con el
 *      Servicio 2 del SIMBE por TCP.
 */
public class DispositivoValidadorCliente {

    private static final int TIMEOUT_MS = 3000;

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";

        Scanner scanner = new Scanner(System.in, "UTF-8");
        System.out.print("idTarjeta (ej: TARJETA-001): ");
        String idTarjeta = limpiar(scanner.nextLine());
        System.out.print("idVehiculo (ej: BUS-10): ");
        String idVehiculo = limpiar(scanner.nextLine());
        System.out.print("idLinea (ej: LINEA-30): ");
        String idLinea = limpiar(scanner.nextLine());
        System.out.print("monto del pasaje (ej: 2300): ");
        double monto = Double.parseDouble(limpiar(scanner.nextLine()));
        String fechaHora = LocalDateTime.now().toString();

        // ---- Paso 1: Servicio 1 - Validar tarjeta (UDP) ----
        JSONObject validacion = new JSONObject();
        validacion.put("idTarjeta", idTarjeta);
        validacion.put("idVehiculo", idVehiculo);
        validacion.put("idLinea", idLinea);
        validacion.put("fechaHora", fechaHora);

        JSONObject respuestaValidacion = validarTarjeta(host, validacion);
        if (respuestaValidacion == null) {
            return; // sin respuesta del Modulo de Validacion no se puede seguir
        }

        boolean tarjetaValida = Boolean.TRUE.equals(respuestaValidacion.get("tarjetaValida"));
        if (!tarjetaValida) {
            System.out.println("[Validador] Tarjeta NO valida (" + respuestaValidacion.get("motivo") + "). No se cobra el pasaje.");
            return;
        }
        System.out.println("[Validador] Tarjeta valida. Saldo disponible: " + respuestaValidacion.get("saldoDisponible"));

        // ---- Paso 2: Servicio 2 - Registrar viaje / cobro de pasaje (TCP) ----
        JSONObject viaje = new JSONObject();
        viaje.put("idTarjeta", idTarjeta);
        viaje.put("idVehiculo", idVehiculo);
        viaje.put("idLinea", idLinea);
        viaje.put("fechaHora", fechaHora);
        viaje.put("monto", monto);

        try (Socket socket = new Socket(host, SimbeServidorCobros.PUERTO);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            System.out.println("[Validador] Enviando por TCP (S2): " + viaje.toJSONString());
            out.println(viaje.toJSONString());

            String linea = in.readLine();
            System.out.println("[Validador] Respuesta del Modulo de Cobro: " + linea);

            // Se muestra la notificacion al pasajero
            JSONObject respuestaCobro = (JSONObject) new JSONParser().parse(linea);
            System.out.println("[Validador] " + respuestaCobro.get("mensaje"));
        }
    }

    /**
     * Envia la solicitud de validacion en un datagrama UDP y espera la
     * respuesta. Como UDP no garantiza la entrega, se pone un timeout para
     * no quedar bloqueado si el datagrama o la respuesta se pierden.
     *
     * @return la respuesta parseada, o null si no hubo respuesta a tiempo
     */
    private static JSONObject validarTarjeta(String host, JSONObject solicitud) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);

            byte[] datos = solicitud.toJSONString().getBytes(StandardCharsets.UTF_8);
            System.out.println("[Validador] Enviando por UDP (S1): " + solicitud.toJSONString());
            socket.send(new DatagramPacket(datos, datos.length, InetAddress.getByName(host), SimbeServidorValidacion.PUERTO));

            byte[] buffer = new byte[SimbeServidorValidacion.TAMANO_BUFFER];
            DatagramPacket respuesta = new DatagramPacket(buffer, buffer.length);
            socket.receive(respuesta);

            String linea = new String(respuesta.getData(), 0, respuesta.getLength(), StandardCharsets.UTF_8);
            System.out.println("[Validador] Respuesta del Modulo de Validacion: " + linea);
            return (JSONObject) new JSONParser().parse(linea);

        } catch (SocketTimeoutException e) {
            System.err.println("[Validador] El Modulo de Validacion no respondio en " + TIMEOUT_MS + " ms");
            return null;
        }
    }

    private static String limpiar(String texto) {
        return texto.replace("﻿", "").trim();
    }
}
