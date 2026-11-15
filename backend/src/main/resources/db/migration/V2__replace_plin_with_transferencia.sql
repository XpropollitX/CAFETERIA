ALTER TABLE pagos
    DROP CONSTRAINT pagos_metodo_pago_check;

UPDATE pagos
SET metodo_pago = 'TRANSFERENCIA'
WHERE metodo_pago = 'PLIN';

ALTER TABLE pagos
    ADD CONSTRAINT ck_pagos_metodo_pago
        CHECK (metodo_pago IN ('EFECTIVO', 'YAPE', 'TRANSFERENCIA'));
