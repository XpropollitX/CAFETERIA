package pe.edu.cafeterialasrosas.mapper;

import org.springframework.stereotype.Component;
import pe.edu.cafeterialasrosas.dto.response.PagoResponse;
import pe.edu.cafeterialasrosas.entity.Pago;

@Component
public class PagoMapper {

    public PagoResponse toResponse(Pago pago) {
        return new PagoResponse(
            pago.getId(),
            pago.getPedido().getId(),
            pago.getMetodoPago(),
            pago.getMonto(),
            pago.getEstadoPago(),
            pago.getRegistradoPor() == null ? null : pago.getRegistradoPor().getId(),
            pago.getFechaPago(),
            pago.getReferencia()
        );
    }
}
