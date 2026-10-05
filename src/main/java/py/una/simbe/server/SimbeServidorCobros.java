package py.una.simbe.server;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor TCP del SIMBE. Representa el Modulo de Cobro de Pasaje
 * (servicio principal) y expone el Servicio 2: Registrar viaje.
 *
 * Se usa TCP porque el cobro modifica el saldo de la tarjeta: se necesita
 * entrega confiable y que el validador reciba si el cobro se hizo o no.
 *
 * IN:  idTarjeta, idVehiculo, idLinea, fechaHora, monto
 * OUT: idViaje, idTarjeta, estado, montoCobrado, saldoRestante, mensaje
 */
public class SimbeServidorCobros {

    public static final int PUERTO = 5003;

    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("[SIMBE] Modulo de Cobro de Pasaje (TCP) escuchando en el puerto " + PUERTO);

            while (true) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("[SIMBE] Conexion aceptada desde " + socketCliente.getRemoteSocketAddress());
                // Cada validador se atiende en su propio hilo
                new Thread(new RegistrarViajeHandler(socketCliente)).start();
            }
        }
    }
}
