package py.una.simbe.client;

import org.json.simple.JSONObject;
import py.una.simbe.server.SimbeServidorRecargas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.UUID;

/**
 * Simula un sistema externo (por ejemplo, una app de recargas) que
 * invoca por TCP el Servicio 5 del SIMBE: Solicitar recarga de tarjeta.
 */
public class SolicitanteRecargaCliente {

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";

        Scanner scanner = new Scanner(System.in, "UTF-8");
        System.out.print("idTarjeta (ej: TARJETA-001): ");
        String idTarjeta = limpiar(scanner.nextLine());
        System.out.print("monto de la recarga (ej: 5000): ");
        double monto = Double.parseDouble(limpiar(scanner.nextLine()));

        JSONObject solicitud = new JSONObject();
        solicitud.put("idTarjeta", idTarjeta);
        solicitud.put("monto", monto);
        solicitud.put("idTransaccion", UUID.randomUUID().toString());
        solicitud.put("fechaHora", LocalDateTime.now().toString());

        try (Socket socket = new Socket(host, SimbeServidorRecargas.PUERTO);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            System.out.println("[Solicitante] Enviando: " + solicitud.toJSONString());
            out.println(solicitud.toJSONString());

            String respuesta = in.readLine();
            System.out.println("[Solicitante] Respuesta del SIMBE: " + respuesta);
        }
    }

    private static String limpiar(String texto) {
        return texto.replace("﻿", "").trim();
    }
}
