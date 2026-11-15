package pe.edu.cafeterialasrosas.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.cafeterialasrosas.entity.MetodoPago;

public record RegistrarPagoRequest(
    @NotNull MetodoPago metodoPago,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal monto,
    @Size(max = 255) String referencia
) {}
