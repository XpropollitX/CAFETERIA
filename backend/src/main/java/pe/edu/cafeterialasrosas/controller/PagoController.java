package pe.edu.cafeterialasrosas.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cafeterialasrosas.dto.response.PagoResponse;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.security.UserPrincipal;
import pe.edu.cafeterialasrosas.service.PagoService;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PatchMapping("/{pagoId}/verificar")
    @PreAuthorize("hasAnyRole('PERSONAL', 'ADMIN')")
    public PagoResponse verificar(
        @PathVariable Long pagoId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return pagoService.verificar(pagoId, actor(principal));
    }

    @PatchMapping("/{pagoId}/anular")
    @PreAuthorize("hasAnyRole('PERSONAL', 'ADMIN')")
    public PagoResponse anular(
        @PathVariable Long pagoId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return pagoService.anular(pagoId, actor(principal));
    }

    private Usuario actor(UserPrincipal principal) {
        Usuario usuario = new Usuario();
        usuario.setId(principal.getId());
        return usuario;
    }
}
