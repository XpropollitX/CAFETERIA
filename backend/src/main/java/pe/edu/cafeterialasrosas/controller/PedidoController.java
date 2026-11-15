package pe.edu.cafeterialasrosas.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cafeterialasrosas.dto.request.CambiarEstadoPedidoRequest;
import pe.edu.cafeterialasrosas.dto.request.CancelarPedidoRequest;
import pe.edu.cafeterialasrosas.dto.request.CrearPedidoRequest;
import pe.edu.cafeterialasrosas.dto.response.HistorialEstadoResponse;
import pe.edu.cafeterialasrosas.dto.response.PedidoResponse;
import pe.edu.cafeterialasrosas.entity.EstadoPedido;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.security.UserPrincipal;
import pe.edu.cafeterialasrosas.service.HistorialEstadoService;
import pe.edu.cafeterialasrosas.service.PedidoService;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final HistorialEstadoService historialEstadoService;

    public PedidoController(PedidoService pedidoService, HistorialEstadoService historialEstadoService) {
        this.pedidoService = pedidoService;
        this.historialEstadoService = historialEstadoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PedidoResponse> crear(
        @Valid @RequestBody CrearPedidoRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(pedidoService.crear(request, actor(principal)));
    }

    @GetMapping("/mis-pedidos")
    @PreAuthorize("hasRole('CLIENTE')")
    public List<PedidoResponse> listarPropios(@AuthenticationPrincipal UserPrincipal principal) {
        return pedidoService.listarPorUsuario(actor(principal));
    }

    @GetMapping("/operativos")
    @PreAuthorize("hasAnyRole('PERSONAL', 'ADMIN')")
    public List<PedidoResponse> listarOperativos(@AuthenticationPrincipal UserPrincipal principal) {
        return pedidoService.listarOperativos(actor(principal));
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PERSONAL', 'ADMIN')")
    public PedidoResponse obtener(
        @PathVariable Long id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return pedidoService.obtener(id, actor(principal));
    }

    @GetMapping("/{id:\\d+}/historial")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PERSONAL', 'ADMIN')")
    public List<HistorialEstadoResponse> listarHistorial(
        @PathVariable Long id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return historialEstadoService.listarPorPedido(id, actor(principal));
    }

    @PatchMapping("/{id:\\d+}/estado")
    @PreAuthorize("hasAnyRole('PERSONAL', 'ADMIN')")
    public PedidoResponse cambiarEstado(
        @PathVariable Long id,
        @Valid @RequestBody CambiarEstadoPedidoRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return pedidoService.cambiarEstado(id, request.estado(), actor(principal), request.observacion());
    }

    @PatchMapping("/{id:\\d+}/cancelar")
    @PreAuthorize("hasAnyRole('PERSONAL', 'ADMIN')")
    public PedidoResponse cancelar(
        @PathVariable Long id,
        @Valid @RequestBody CancelarPedidoRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return pedidoService.cambiarEstado(id, EstadoPedido.CANCELADO, actor(principal), request.observacion());
    }

    private Usuario actor(UserPrincipal principal) {
        Usuario usuario = new Usuario();
        usuario.setId(principal.getId());
        return usuario;
    }
}
