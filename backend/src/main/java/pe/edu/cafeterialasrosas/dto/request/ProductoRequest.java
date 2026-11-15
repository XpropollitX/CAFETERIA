package pe.edu.cafeterialasrosas.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
    @NotNull @Positive Long categoriaId,
    @NotBlank @Size(max = 150) String nombre,
    @Size(max = 1000) String descripcion,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal precio,
    @Size(max = 2048) String imagenUrl,
    @NotNull Boolean disponible
) {}
