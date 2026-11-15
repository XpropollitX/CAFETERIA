package pe.edu.cafeterialasrosas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DetallePedidoRequest(
    @NotNull @Positive Long productoId,
    @Positive int cantidad
) {}
