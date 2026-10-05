-- Tablas para el Modulo de Validacion (S1) y el Modulo de Cobro de Pasaje (S2).
-- Ejecutar despues de 01_crear_tabla.sql.
-- No tienen FK hacia tarjetas a proposito: asi tambien se registran los
-- intentos hechos con tarjetas inexistentes.

-- Cada validacion (S1, UDP) que hace un Dispositivo Validador queda registrada aca.
CREATE TABLE validaciones (
    id_validacion    SERIAL        PRIMARY KEY,
    id_tarjeta       VARCHAR(30)   NOT NULL,
    id_vehiculo      VARCHAR(30)   NOT NULL,
    id_linea         VARCHAR(30)   NOT NULL,
    fecha_hora       TIMESTAMP     NOT NULL,              -- fecha/hora informada por el validador
    tarjeta_valida   BOOLEAN       NOT NULL,
    motivo           VARCHAR(40)   NOT NULL,              -- OK / TARJETA_INEXISTENTE / TARJETA_INHABILITADA
    saldo_disponible NUMERIC(10,2) NOT NULL,              -- saldo al momento de validar
    fecha_registro   TIMESTAMP     NOT NULL DEFAULT now() -- cuando lo guardo el servidor
);

-- Cada intento de cobro de pasaje (S2, TCP), haya sido cobrado o rechazado.
CREATE TABLE viajes (
    id_viaje         SERIAL        PRIMARY KEY,
    id_tarjeta       VARCHAR(30)   NOT NULL,
    id_vehiculo      VARCHAR(30)   NOT NULL,
    id_linea         VARCHAR(30)   NOT NULL,
    fecha_hora       TIMESTAMP     NOT NULL,              -- fecha/hora informada por el validador
    monto            NUMERIC(10,2) NOT NULL,              -- precio del pasaje solicitado
    estado           VARCHAR(30)   NOT NULL,              -- COBRADO / SALDO_INSUFICIENTE / TARJETA_INEXISTENTE / TARJETA_INHABILITADA
    monto_cobrado    NUMERIC(10,2) NOT NULL,              -- 0 si no se cobro
    saldo_restante   NUMERIC(10,2) NOT NULL,              -- saldo de la tarjeta luego del intento
    fecha_registro   TIMESTAMP     NOT NULL DEFAULT now()
);
