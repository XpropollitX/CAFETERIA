package pe.edu.cafeterialasrosas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarPedidoRequest(
    @NotBlank @Size(max = 1000) String observacion
) {}
