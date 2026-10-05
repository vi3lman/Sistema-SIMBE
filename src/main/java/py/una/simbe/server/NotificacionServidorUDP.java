package py.una.simbe.server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import py.una.BD.NotificacionDAO;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Servidor UDP del SIMBE. Representa el Modulo de Notificaciones y
 * recibe el Servicio 5 de SPPE: Notificar resultado de pago.
 *
 * A diferencia de SimbeServidorRecargas (TCP, puerto 5001), este es
 * un proceso Java INDEPENDIENTE: tiene su propio main() y su propio
 * puerto. Hay que ejecutarlo en su propia terminal.
 */
public class NotificacionServidorUDP {

    public static final int PUERTO = Integer.parseInt(System.getProperty("simbe.notif.port", "6002"));
    private static final NotificacionDAO notificacionDAO = new NotificacionDAO();

    public static void main(String[] args) throws Exception {
        DatagramSocket serverSocket = new DatagramSocket(PUERTO);
        System.out.println("[SIMBE] Modulo de Notificaciones (UDP) escuchando en el puerto " + PUERTO);

        while (true) {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            serverSocket.receive(receivePacket); // bloqueante

            String datoRecibido = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
            System.out.println("[SIMBE] Notificacion recibida: " + datoRecibido);

            InetAddress ipOrigen = receivePacket.getAddress();
            int puertoOrigen = receivePacket.getPort();

            String idNotificacion = "NOT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            try {
                JSONObject n = (JSONObject) new JSONParser().parse(datoRecibido);
                String idTransaccion = (String) n.get("idTransaccion");
                String estado = (String) n.get("estado");
                double monto = ((Number) n.get("monto")).doubleValue();
                String codigoResultado = (String) n.get("codigoResultado");
                String fechaHora = (String) n.get("fechaHora");

                notificacionDAO.registrar(idNotificacion, idTransaccion, estado, monto, codigoResultado, fechaHora);
                System.out.println("[SIMBE] Notificacion persistida: idTransaccion=" + idTransaccion + ", estado=" + estado);

            } catch (Exception e) {
                System.err.println("[SIMBE] Error procesando/persistiendo notificacion: " + e.getMessage());
            }

            JSONObject ack = new JSONObject();
            ack.put("recibido", true);
            ack.put("idNotificacion", idNotificacion);

            byte[] sendData = ack.toJSONString().getBytes(StandardCharsets.UTF_8);
            serverSocket.send(new DatagramPacket(sendData, sendData.length, ipOrigen, puertoOrigen));
        }
    }
}