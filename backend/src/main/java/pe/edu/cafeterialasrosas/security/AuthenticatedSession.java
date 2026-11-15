package pe.edu.cafeterialasrosas.security;

import pe.edu.cafeterialasrosas.dto.response.UsuarioResponse;

public record AuthenticatedSession(UsuarioResponse usuario, String token) {}
