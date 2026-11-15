package pe.edu.cafeterialasrosas.dto.request;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CrearPedidoRequest(
    @NotEmpty List<@Valid DetallePedidoRequest> detalles,
    @Size(max = 1000) String observaciones
) {}
