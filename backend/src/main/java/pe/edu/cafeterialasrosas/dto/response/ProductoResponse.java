package pe.edu.cafeterialasrosas.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductoResponse(
    Long id,
    Long categoriaId,
    String categoria,
    String nombre,
    String descripcion,
    BigDecimal precio,
    String imagenUrl,
    boolean disponible,
    Instant fechaCreacion
) {}
