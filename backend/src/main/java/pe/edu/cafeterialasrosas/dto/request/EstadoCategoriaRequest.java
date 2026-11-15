package pe.edu.cafeterialasrosas.dto.request;

import jakarta.validation.constraints.NotNull;

public record EstadoCategoriaRequest(@NotNull Boolean activo) {}
