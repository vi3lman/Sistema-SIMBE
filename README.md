# SIMBE - Sistema Metropolitano de Billetaje Electronico

Proyecto Maven correspondiente al Sistema 1 (SIMBE)

Organizacion propietaria: Empresa Metropolitana de Transporte Electronico
S.A. (EMTE).

## Estado actual

Implementacion del **Servicio 5: Solicitar recarga de tarjeta** con
sockets **TCP**, incluyendo la comunicacion real entre los dos sistemas:
al recibir una solicitud de recarga, el **Modulo de Recargas** de SIMBE
se conecta por TCP al **Modulo de Cobros** de **SPPE** (Sistema 2, en el
otro repositorio) para invocar su Servicio 1 ("Procesar pago") y recien
en base a esa respuesta acredita el saldo.

```
Cliente externo (simulador)        SIMBE (este repo)                   SPPE (otro repo)
  SolicitanteRecargaCliente --TCP-->  SimbeServidorRecargas
                                          -> SolicitarRecargaHandler
                                               -> SppeClient --TCP-->  Modulo de Cobros
                                               <---------- respuesta ----------
  <----------- respuesta S5 -----------
```

- **Entrada (Servicio 5):** `idTarjeta`, `monto`, `idTransaccion`, `fechaHora`
- **Salida (Servicio 5):** `idTarjeta`, `montoRecargado`, `nuevoSaldo`, `estado`
  (`APROBADO` / `ERROR_COMUNICACION_SPPE`)

El saldo de las tarjetas se simula en memoria (`SaldoTarjetas`), con dos
tarjetas de prueba: `TARJETA-001` (15000) y `TARJETA-002` (3000).

### Contrato hacia SPPE (Servicio 1: Procesar pago)

`SppeClient` envia por TCP el siguiente JSON, y espera una respuesta con
la misma forma que documenta SPPE para su Servicio 1:

- **Envia:** `idCliente` (= idTarjeta), `idMedioPago` (= idTransaccion),
  `monto`, `concepto`, `fechaHora`
- **Espera recibir:** `idTransaccion`, `estado` (`APROBADO`/`OK` para
  considerarlo exitoso), `montoProcesado`, `fechaHora`, `motivo`

Mientras el servidor SPPE no exista o no este corriendo, `SppeClient`
falla de forma controlada (timeout/`ConnectException`) y el Servicio 5
responde con `estado = "ERROR_COMUNICACION_SPPE"` en vez de que SIMBE se
caiga. Esto es esperado hasta integrar el otro repositorio.

Host y puerto de SPPE son configurables via propiedades del sistema
(default `localhost:6001`):

```bash
-Dsppe.host=localhost -Dsppe.port=6001
```

Los demas servicios (Validar tarjeta, Registrar viaje, Consultar tarifa,
Actualizar estado de tarjeta) quedan pendientes para una proxima entrega.

## Estructura

```
simbe-billetaje/
├── pom.xml
├── .gitignore
└── src/
    └── main/
        ├── java/py/una/simbe/
        │   ├── server/
        │   │   ├── SimbeServidorRecargas.java   (servidor TCP, Modulo de Recargas)
        │   │   ├── SolicitarRecargaHandler.java (atiende el Servicio 5 por conexion)
        │   │   └── SaldoTarjetas.java           (saldo simulado en memoria)
        │   └── client/
        │       ├── SppeClient.java              (cliente TCP interno hacia SPPE)
        │       └── SolicitanteRecargaCliente.java (simula al sistema externo)
        └── resources/
```

## Compilar

```bash
mvn clean package
```

## Ejecutar

En una terminal, iniciar el servidor (Modulo de Recargas del SIMBE):

```bash
mvn compile exec:java -Dexec.mainClass=py.una.simbe.server.SimbeServidorRecargas
```

En otra terminal, ejecutar el cliente que simula al sistema externo
solicitando la recarga:

```bash
mvn compile exec:java -Dexec.mainClass=py.una.simbe.client.SolicitanteRecargaCliente
```

El cliente pedira interactivamente el `idTarjeta` y el `monto` de la
recarga, y mostrara la respuesta del servidor. Hasta que el servidor de
SPPE (Sistema 2) este implementado y corriendo, la respuesta llegara con
`estado = "ERROR_COMUNICACION_SPPE"` — eso confirma que el primer tramo
(cliente externo -> SIMBE) funciona correctamente.

## Integrantes

- Cesar Vielman
- Richar Carballo
