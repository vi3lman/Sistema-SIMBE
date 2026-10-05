CREATE TABLE tarjetas (
    id_tarjeta          VARCHAR(30) PRIMARY KEY,
    saldo               NUMERIC(10,2) NOT NULL DEFAULT 0,
    estado              VARCHAR(20)   NOT NULL DEFAULT 'HABILITADA',
    fecha_actualizacion TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE TABLE notificaciones_pago (
    id_notificacion   VARCHAR(50) PRIMARY KEY,
    id_transaccion    VARCHAR(50) NOT NULL,
    estado            VARCHAR(20) NOT NULL,
    monto             NUMERIC(10,2),
    codigo_resultado  VARCHAR(30),
    fecha_hora        VARCHAR(40),
    fecha_recepcion   TIMESTAMP NOT NULL DEFAULT now()
);
-- Mismos datos de prueba que tenias en SaldoTarjetas
INSERT INTO tarjetas (id_tarjeta, saldo, estado) VALUES
('TARJETA-001', 15000, 'HABILITADA'),
('TARJETA-002', 3000,  'HABILITADA');