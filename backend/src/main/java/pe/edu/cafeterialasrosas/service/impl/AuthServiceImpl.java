package pe.edu.cafeterialasrosas.service.impl;

import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import pe.edu.cafeterialasrosas.dto.request.LoginRequest;
import pe.edu.cafeterialasrosas.dto.response.UsuarioResponse;
import pe.edu.cafeterialasrosas.security.AuthenticatedSession;
import pe.edu.cafeterialasrosas.security.JwtService;
import pe.edu.cafeterialasrosas.security.UserPrincipal;
import pe.edu.cafeterialasrosas.service.UsuarioService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Service
public class AuthServiceImpl implements pe.edu.cafeterialasrosas.service.AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public AuthServiceImpl(
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        UsuarioService usuarioService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @Override
    public AuthenticatedSession iniciarSesion(LoginRequest request) {
        if (request == null || request.correo() == null || request.correo().isBlank()
            || request.password() == null || request.password().isBlank()) {
            throw new BadCredentialsException("Correo o contraseña inválidos.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                    request.correo().trim().toLowerCase(Locale.ROOT),
                    request.password()
                )
            );
        } catch (AuthenticationException exception) {
            throw new BadCredentialsException("Correo o contraseña inválidos.");
        }

        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthenticationServiceException("No se pudo completar la autenticación.");
        }

        UsuarioResponse usuario = usuarioService.obtenerPorId(principal.getId());
        return new AuthenticatedSession(usuario, jwtService.generateToken(principal));
    }
}
