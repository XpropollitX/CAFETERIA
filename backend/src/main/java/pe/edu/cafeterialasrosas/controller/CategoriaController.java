package pe.edu.cafeterialasrosas.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cafeterialasrosas.dto.request.CategoriaRequest;
import pe.edu.cafeterialasrosas.dto.request.EstadoCategoriaRequest;
import pe.edu.cafeterialasrosas.dto.response.CategoriaResponse;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.security.UserPrincipal;
import pe.edu.cafeterialasrosas.service.CategoriaService;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'PERSONAL', 'ADMIN')")
    public List<CategoriaResponse> listar(@AuthenticationPrincipal UserPrincipal principal) {
        return esAdmin(principal) ? categoriaService.listarTodas() : categoriaService.listarActivas();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PERSONAL', 'ADMIN')")
    public CategoriaResponse obtener(
        @PathVariable Long id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return esAdmin(principal)
            ? categoriaService.obtenerPorId(id)
            : categoriaService.obtenerActivaPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponse> crear(
        @Valid @RequestBody CategoriaRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(categoriaService.crear(request, actor(principal)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoriaResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody CategoriaRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return categoriaService.actualizar(id, request, actor(principal));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoriaResponse cambiarEstado(
        @PathVariable Long id,
        @Valid @RequestBody EstadoCategoriaRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return categoriaService.cambiarEstado(id, request.activo(), actor(principal));
    }

    private Usuario actor(UserPrincipal principal) {
        Usuario usuario = new Usuario();
        usuario.setId(principal.getId());
        return usuario;
    }

    private boolean esAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
    }
}
