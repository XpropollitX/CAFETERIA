package pe.edu.cafeterialasrosas.service;

import java.util.List;
import pe.edu.cafeterialasrosas.dto.response.HistorialEstadoResponse;
import pe.edu.cafeterialasrosas.entity.EstadoPedido;
import pe.edu.cafeterialasrosas.entity.Pedido;
import pe.edu.cafeterialasrosas.entity.Usuario;

public interface HistorialEstadoService {

    HistorialEstadoResponse registrarCambio(
        Pedido pedido,
        EstadoPedido estado,
        Usuario usuario,
        String observacion
    );

    List<HistorialEstadoResponse> listarPorPedido(Long pedidoId, Usuario solicitante);
}
