package pe.edu.cafeterialasrosas.dto.response;

import java.time.Instant;

public record UsuarioResponse(
    Long id,
    String nombres,
    String apellidos,
    String correo,
    String telefono,
    String rol,
    boolean activo,
    Instant fechaCreacion
) {}
