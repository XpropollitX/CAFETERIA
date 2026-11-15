package pe.edu.cafeterialasrosas.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cafeterialasrosas.dto.request.RegistrarPagoRequest;
import pe.edu.cafeterialasrosas.dto.response.PagoResponse;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.security.UserPrincipal;
import pe.edu.cafeterialasrosas.service.PagoService;

@RestController
@RequestMapping("/api/pedidos/{pedidoId}/pagos")
public class PagoPedidoController {

    private final PagoService pagoService;

    public PagoPedidoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PagoResponse> registrar(
        @PathVariable Long pedidoId,
        @Valid @RequestBody RegistrarPagoRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(pagoService.registrarIntencion(pedidoId, actor(principal), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'PERSONAL', 'ADMIN')")
    public List<PagoResponse> listar(
        @PathVariable Long pedidoId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return pagoService.listarPorPedido(pedidoId, actor(principal));
    }

    private Usuario actor(UserPrincipal principal) {
        Usuario usuario = new Usuario();
        usuario.setId(principal.getId());
        return usuario;
    }
}
