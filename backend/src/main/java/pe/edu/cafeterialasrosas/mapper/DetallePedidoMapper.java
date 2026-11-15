package pe.edu.cafeterialasrosas.mapper;

import org.springframework.stereotype.Component;
import pe.edu.cafeterialasrosas.dto.response.DetallePedidoResponse;
import pe.edu.cafeterialasrosas.entity.DetallePedido;

@Component
public class DetallePedidoMapper {

    public DetallePedidoResponse toResponse(DetallePedido detalle) {
        return new DetallePedidoResponse(
            detalle.getId(),
            detalle.getProducto().getId(),
            detalle.getProducto().getNombre(),
            detalle.getCantidad(),
            detalle.getPrecioUnitario(),
            detalle.getSubtotal()
        );
    }
}
