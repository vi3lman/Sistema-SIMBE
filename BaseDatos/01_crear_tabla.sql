CREATE TABLE tarjetas (
    id_tarjeta          VARCHAR(30) PRIMARY KEY,
    saldo               NUMERIC(10,2) NOT NULL DEFAULT 0,
    estado              VARCHAR(20)   NOT NULL DEFAULT 'HABILITADA',
    fecha_actualizacion TIMESTAMP     NOT NULL DEFAULT now()
);

-- Mismos datos de prueba que tenias en SaldoTarjetas
INSERT INTO tarjetas (id_tarjeta, saldo, estado) VALUES
('TARJETA-001', 15000, 'HABILITADA'),
('TARJETA-002', 3000,  'HABILITADA');