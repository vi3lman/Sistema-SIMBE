package py.una.simbe.server;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor TCP del SIMBE. Representa el Modulo de Recargas y expone el
 * Servicio 5: Solicitar recarga de tarjeta.
 *
 * Por cada solicitud de recarga se atiende la conexion en un hilo aparte.
 * Internamente, cada solicitud dispara una llamada TCP saliente hacia
 * SPPE (Sistema 2) para procesar el cobro que financia la recarga.
 */
public class SimbeServidorRecargas {

    public static final int PUERTO = 5001;

    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("[SIMBE] Modulo de Recargas escuchando en el puerto " + PUERTO);

            while (true) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("[SIMBE] Conexion aceptada desde " + socketCliente.getRemoteSocketAddress());
                new Thread(new SolicitarRecargaHandler(socketCliente)).start();
            }
        }
    }
}
