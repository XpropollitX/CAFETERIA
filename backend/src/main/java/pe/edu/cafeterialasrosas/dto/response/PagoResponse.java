package pe.edu.cafeterialasrosas.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import pe.edu.cafeterialasrosas.entity.EstadoPago;
import pe.edu.cafeterialasrosas.entity.MetodoPago;

public record PagoResponse(
    Long id,
    Long pedidoId,
    MetodoPago metodoPago,
    BigDecimal monto,
    EstadoPago estadoPago,
    Long registradoPorId,
    Instant fechaPago,
    String referencia
) {}
