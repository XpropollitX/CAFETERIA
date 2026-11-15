package pe.edu.cafeterialasrosas.controller;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cafeterialasrosas.dto.request.LoginRequest;
import pe.edu.cafeterialasrosas.dto.request.RegistrarUsuarioRequest;
import pe.edu.cafeterialasrosas.dto.response.AuthResponse;
import pe.edu.cafeterialasrosas.dto.response.CsrfTokenResponse;
import pe.edu.cafeterialasrosas.dto.response.UsuarioResponse;
import pe.edu.cafeterialasrosas.security.AuthCookieService;
import pe.edu.cafeterialasrosas.security.AuthenticatedSession;
import pe.edu.cafeterialasrosas.security.UserPrincipal;
import pe.edu.cafeterialasrosas.service.AuthService;
import pe.edu.cafeterialasrosas.service.UsuarioService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final AuthCookieService authCookieService;

    public AuthController(
        AuthService authService,
        UsuarioService usuarioService,
        AuthCookieService authCookieService
    ) {
        this.authService = authService;
        this.usuarioService = usuarioService;
        this.authCookieService = authCookieService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponse> csrfToken(CsrfToken csrfToken) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName()));
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> register(
        @Valid @RequestBody RegistrarUsuarioRequest request
    ) {
        UsuarioResponse usuario = usuarioService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedSession session = authService.iniciarSesion(request);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, authCookieService.create(session.token()).toString())
            .cacheControl(CacheControl.noStore())
            .body(new AuthResponse(session.usuario()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, authCookieService.clear().toString())
            .cacheControl(CacheControl.noStore())
            .build();
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return usuarioService.obtenerPorId(principal.getId());
    }
}
