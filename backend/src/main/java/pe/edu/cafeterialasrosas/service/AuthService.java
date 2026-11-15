package pe.edu.cafeterialasrosas.service;

import pe.edu.cafeterialasrosas.dto.request.LoginRequest;
import pe.edu.cafeterialasrosas.security.AuthenticatedSession;

public interface AuthService {

    AuthenticatedSession iniciarSesion(LoginRequest request);
}
