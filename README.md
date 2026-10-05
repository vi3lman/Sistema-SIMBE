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

El saldo de las tarjetas se guarda en PostgreSQL (tabla `tarjetas`, script
`BaseDatos/01_crear_tabla.sql`), con dos tarjetas de prueba:
`TARJETA-001` (15000) y `TARJETA-002` (3000).

### Servicio 1: Validar tarjeta (UDP, puerto 5002)

Modulo de Validacion (`SimbeServidorValidacion` + `ValidarTarjetaHandler`).
La tarjeta es valida si existe y su estado es `HABILITADA`. Cada validacion
se registra en la tabla `validaciones`.

- **Entrada:** `idTarjeta`, `idVehiculo`, `idLinea`, `fechaHora`
- **Salida:** `idValidacion`, `idTarjeta`, `tarjetaValida`, `motivo`
  (`OK` / `TARJETA_INEXISTENTE` / `TARJETA_INHABILITADA` / `ERROR_BASE_DATOS`),
  `saldoDisponible`

### Servicio 2: Registrar viaje - cobro de pasaje (TCP, puerto 5003)

Modulo de Cobro de Pasaje (`SimbeServidorCobros` + `RegistrarViajeHandler`).
Si el saldo alcanza, descuenta el pasaje; si no, responde
`SALDO_INSUFICIENTE` sin tocar el saldo. El descuento es atomico
(`UPDATE ... WHERE saldo >= monto`) y se hace en la misma transaccion que
el registro del viaje en la tabla `viajes`.

- **Entrada:** `idTarjeta`, `idVehiculo`, `idLinea`, `fechaHora`, `monto`
- **Salida:** `idViaje`, `idTarjeta`, `estado` (`COBRADO` /
  `SALDO_INSUFICIENTE` / `TARJETA_INEXISTENTE` / `TARJETA_INHABILITADA` /
  `ERROR_BASE_DATOS`), `montoCobrado`, `saldoRestante`, `mensaje`
  (notificacion del descuento o del rechazo)

```
DispositivoValidadorCliente --UDP S1-->  SimbeServidorValidacion -> ValidarTarjetaHandler
                            --TCP S2-->  SimbeServidorCobros     -> RegistrarViajeHandler -> ViajeDAO
```

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

Bash:

```bash
-Dsppe.host=localhost -Dsppe.port=6001
```

PowerShell (entre comillas, ver nota en [Ejecutar](#ejecutar)):

```powershell
"-Dsppe.host=localhost" "-Dsppe.port=6001"
```

Los demas servicios (Consultar tarifa, Actualizar estado de tarjeta)
quedan pendientes para una proxima entrega.

## Estructura

```
simbe-billetaje/
├── pom.xml
├── BaseDatos/
│   ├── 01_crear_tabla.sql            (tabla tarjetas + datos de prueba)
│   └── 02_viajes_validaciones.sql    (tablas validaciones y viajes)
└── src/
    └── main/
        └── java/py/una/
            ├── BD/
            │   ├── Bd.java                  (conexion a PostgreSQL)
            │   ├── TarjetaDAO.java          (saldo/estado de tarjetas)
            │   ├── ValidacionDAO.java       (registro de validaciones, S1)
            │   └── ViajeDAO.java            (cobro de pasaje y registro de viajes, S2)
            └── simbe/
                ├── server/
                │   ├── SimbeServidorRecargas.java   (servidor TCP, Modulo de Recargas)
                │   ├── SolicitarRecargaHandler.java (atiende el Servicio 5 por conexion)
                │   ├── SimbeServidorValidacion.java (servidor UDP, Modulo de Validacion)
                │   ├── ValidarTarjetaHandler.java   (atiende el Servicio 1 por datagrama)
                │   ├── SimbeServidorCobros.java     (servidor TCP, Modulo de Cobro de Pasaje)
                │   └── RegistrarViajeHandler.java   (atiende el Servicio 2 por conexion)
                └── client/
                    ├── SppeClient.java                  (cliente TCP interno hacia SPPE)
                    ├── SolicitanteRecargaCliente.java   (simula al sistema externo de recargas)
                    └── DispositivoValidadorCliente.java (simula el validador del vehiculo)
```

## Base de datos

```bash
psql -U postgres -d simbe_db -f BaseDatos/01_crear_tabla.sql
psql -U postgres -d simbe_db -f BaseDatos/02_viajes_validaciones.sql
```

## Compilar

```bash
mvn clean package
```

## Ejecutar

> **Nota para PowerShell:** los argumentos `-D...` van entre comillas
> (`"-Dexec.mainClass=..."`). Sin comillas, PowerShell corta el argumento
> en el primer `.` y Maven falla con `Unknown lifecycle phase ".mainClass=..."`.
> En Bash (Git Bash, Linux, macOS) y en `cmd` no hace falta.

En una terminal, iniciar el servidor (Modulo de Recargas del SIMBE):

Bash:

```bash
mvn compile exec:java -Dexec.mainClass=py.una.simbe.server.SimbeServidorRecargas
```

PowerShell:

```powershell
mvn compile exec:java "-Dexec.mainClass=py.una.simbe.server.SimbeServidorRecargas"
```

En otra terminal, ejecutar el cliente que simula al sistema externo
solicitando la recarga:

Bash:

```bash
mvn compile exec:java -Dexec.mainClass=py.una.simbe.client.SolicitanteRecargaCliente
```

PowerShell:

```powershell
mvn compile exec:java "-Dexec.mainClass=py.una.simbe.client.SolicitanteRecargaCliente"
```

El cliente pedira interactivamente el `idTarjeta` y el `monto` de la
recarga, y mostrara la respuesta del servidor. Hasta que el servidor de
SPPE (Sistema 2) este implementado y corriendo, la respuesta llegara con
`estado = "ERROR_COMUNICACION_SPPE"` — eso confirma que el primer tramo
(cliente externo -> SIMBE) funciona correctamente.

### Validacion y cobro de pasaje (S1 + S2)

Levantar cada modulo en su propia terminal (un comando por terminal):

Bash:

```bash
mvn compile exec:java -Dexec.mainClass=py.una.simbe.server.SimbeServidorValidacion
mvn compile exec:java -Dexec.mainClass=py.una.simbe.server.SimbeServidorCobros
```

PowerShell:

```powershell
mvn compile exec:java "-Dexec.mainClass=py.una.simbe.server.SimbeServidorValidacion"
mvn compile exec:java "-Dexec.mainClass=py.una.simbe.server.SimbeServidorCobros"
```

En otra terminal, simular el validador del vehiculo:

Bash:

```bash
mvn compile exec:java -Dexec.mainClass=py.una.simbe.client.DispositivoValidadorCliente
```

PowerShell:

```powershell
mvn compile exec:java "-Dexec.mainClass=py.una.simbe.client.DispositivoValidadorCliente"
```

Pide `idTarjeta`, `idVehiculo`, `idLinea` y `monto`; primero valida la
tarjeta por UDP y, si es valida, registra el viaje por TCP. Ejemplo con
`TARJETA-002` y pasaje 2300: la primera vez responde `COBRADO` con saldo
restante 700; la segunda, `SALDO_INSUFICIENTE`.

## Integrantes

- Cesar Vielman
- Richar Carballo
