package pe.edu.cafeterialasrosas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.cafeterialasrosas.entity.EstadoPedido;

public record CambiarEstadoPedidoRequest(
    @NotNull EstadoPedido estado,
    @Size(max = 1000) String observacion
) {}
