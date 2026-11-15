package pe.edu.cafeterialasrosas.dto.response;

import java.time.Instant;
import pe.edu.cafeterialasrosas.entity.EstadoPedido;

public record HistorialEstadoResponse(
    Long id,
    Long pedidoId,
    EstadoPedido estado,
    Long usuarioId,
    Instant fechaCambio,
    String observacion
) {}
