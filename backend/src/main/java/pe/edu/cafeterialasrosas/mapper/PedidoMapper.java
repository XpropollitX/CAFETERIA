package pe.edu.cafeterialasrosas.mapper;

import org.springframework.stereotype.Component;
import pe.edu.cafeterialasrosas.dto.response.PedidoResponse;
import pe.edu.cafeterialasrosas.entity.Pedido;

@Component
public class PedidoMapper {

    private final DetallePedidoMapper detallePedidoMapper;

    public PedidoMapper(DetallePedidoMapper detallePedidoMapper) {
        this.detallePedidoMapper = detallePedidoMapper;
    }

    public PedidoResponse toResponse(Pedido pedido) {
        return new PedidoResponse(
            pedido.getId(),
            pedido.getUsuario().getId(),
            pedido.getFechaPedido(),
            pedido.getEstado(),
            pedido.getSubtotal(),
            pedido.getTotal(),
            pedido.getObservaciones(),
            pedido.getDetalles().stream().map(detallePedidoMapper::toResponse).toList()
        );
    }
}
