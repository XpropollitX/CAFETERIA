package pe.edu.cafeterialasrosas.service;

import java.util.List;
import pe.edu.cafeterialasrosas.dto.request.RegistrarPagoRequest;
import pe.edu.cafeterialasrosas.dto.response.PagoResponse;
import pe.edu.cafeterialasrosas.entity.Usuario;

public interface PagoService {

    PagoResponse registrarIntencion(Long pedidoId, Usuario cliente, RegistrarPagoRequest request);

    List<PagoResponse> listarPorPedido(Long pedidoId, Usuario solicitante);

    PagoResponse verificar(Long pagoId, Usuario verificador);

    PagoResponse anular(Long pagoId, Usuario responsable);
}
