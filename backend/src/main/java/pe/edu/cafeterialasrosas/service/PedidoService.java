package pe.edu.cafeterialasrosas.service;

import java.util.List;
import pe.edu.cafeterialasrosas.dto.request.CrearPedidoRequest;
import pe.edu.cafeterialasrosas.dto.response.PedidoResponse;
import pe.edu.cafeterialasrosas.entity.EstadoPedido;
import pe.edu.cafeterialasrosas.entity.Usuario;

public interface PedidoService {

    PedidoResponse crear(CrearPedidoRequest request, Usuario cliente);

    PedidoResponse obtener(Long pedidoId, Usuario solicitante);

    List<PedidoResponse> listarPorUsuario(Usuario solicitante);

    List<PedidoResponse> listarOperativos(Usuario responsable);

    List<PedidoResponse> listarPorEstado(EstadoPedido estado, Usuario responsable);

    PedidoResponse cambiarEstado(
        Long pedidoId,
        EstadoPedido nuevoEstado,
        Usuario responsable,
        String observacion
    );
}
