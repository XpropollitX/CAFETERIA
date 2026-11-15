package pe.edu.cafeterialasrosas.service;

import java.util.List;
import pe.edu.cafeterialasrosas.dto.response.DetallePedidoResponse;
import pe.edu.cafeterialasrosas.entity.Usuario;

public interface DetallePedidoService {

    List<DetallePedidoResponse> listarPorPedido(Long pedidoId, Usuario solicitante);
}
