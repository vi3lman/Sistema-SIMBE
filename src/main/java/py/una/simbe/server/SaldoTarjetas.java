package py.una.simbe.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacen en memoria del saldo de las tarjetas, unicamente para poder
 * demostrar el Servicio 5 (Solicitar recarga de tarjeta) sin depender de
 * una base de datos real.
 */
public class SaldoTarjetas {

    private static final Map<String, Double> SALDOS = new ConcurrentHashMap<>();

    static {
        SALDOS.put("TARJETA-001", 15000.0);
        SALDOS.put("TARJETA-002", 3000.0);
    }

    private SaldoTarjetas() {
    }

    public static synchronized double obtenerSaldo(String idTarjeta) {
        return SALDOS.getOrDefault(idTarjeta, 0.0);
    }

    public static synchronized double acreditar(String idTarjeta, double monto) {
        double nuevoSaldo = obtenerSaldo(idTarjeta) + monto;
        SALDOS.put(idTarjeta, nuevoSaldo);
        return nuevoSaldo;
    }
}
