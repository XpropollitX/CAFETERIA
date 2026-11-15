package pe.edu.cafeterialasrosas.dto.response;

import java.math.BigDecimal;

public record DetallePedidoResponse(
    Long id,
    Long productoId,
    String producto,
    int cantidad,
    BigDecimal precioUnitario,
    BigDecimal subtotal
) {}
