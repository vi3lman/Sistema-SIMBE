package py.una.simbe.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Servidor UDP del SIMBE. Representa el Modulo de Validacion y expone el
 * Servicio 1: Validar tarjeta.
 *
 * Se usa UDP porque la validacion es una consulta corta y rapida (un
 * datagrama de ida y uno de vuelta), sin necesidad de mantener conexion.
 *
 * IN:  idTarjeta, idVehiculo, idLinea, fechaHora
 * OUT: idValidacion, idTarjeta, tarjetaValida, motivo, saldoDisponible
 */
public class SimbeServidorValidacion {

    public static final int PUERTO = 5002;
    public static final int TAMANO_BUFFER = 1024;

    public static void main(String[] args) throws Exception {
        try (DatagramSocket socket = new DatagramSocket(PUERTO)) {
            System.out.println("[SIMBE] Modulo de Validacion (UDP) escuchando en el puerto " + PUERTO);

            while (true) {
                // Buffer nuevo por cada datagrama: asi el hilo que lo atiende
                // no comparte el arreglo con la siguiente recepcion
                byte[] buffer = new byte[TAMANO_BUFFER];
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                socket.receive(paquete); // bloquea hasta que llega un datagrama

                System.out.println("[SIMBE] Datagrama recibido desde " + paquete.getSocketAddress());
                // Cada validacion se procesa en un hilo aparte; el mismo socket
                // se usa para responder (DatagramSocket.send es thread-safe)
                new Thread(new ValidarTarjetaHandler(socket, paquete)).start();
            }
        }
    }
}
