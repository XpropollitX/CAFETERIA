package pe.edu.cafeterialasrosas.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import pe.edu.cafeterialasrosas.entity.EstadoPedido;

public record PedidoResponse(
    Long id,
    Long usuarioId,
    Instant fechaPedido,
    EstadoPedido estado,
    BigDecimal subtotal,
    BigDecimal total,
    String observaciones,
    List<DetallePedidoResponse> detalles
) {}
