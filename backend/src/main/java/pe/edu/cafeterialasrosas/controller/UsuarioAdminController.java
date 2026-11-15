package pe.edu.cafeterialasrosas.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cafeterialasrosas.dto.request.RegistrarUsuarioRequest;
import pe.edu.cafeterialasrosas.dto.response.UsuarioResponse;
import pe.edu.cafeterialasrosas.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioAdminController {

    private final UsuarioService usuarioService;

    public UsuarioAdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/personal")
    public ResponseEntity<UsuarioResponse> crearPersonal(
        @Valid @RequestBody RegistrarUsuarioRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(usuarioService.registrarPersonal(request));
    }
}
