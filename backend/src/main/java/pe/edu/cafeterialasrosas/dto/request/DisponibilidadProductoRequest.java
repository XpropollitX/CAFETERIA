package pe.edu.cafeterialasrosas.dto.request;

import jakarta.validation.constraints.NotNull;

public record DisponibilidadProductoRequest(@NotNull Boolean disponible) {}
