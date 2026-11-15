package pe.edu.cafeterialasrosas.mapper;

import org.springframework.stereotype.Component;
import pe.edu.cafeterialasrosas.dto.response.HistorialEstadoResponse;
import pe.edu.cafeterialasrosas.entity.HistorialEstadoPedido;

@Component
public class HistorialEstadoMapper {

    public HistorialEstadoResponse toResponse(HistorialEstadoPedido historial) {
        return new HistorialEstadoResponse(
            historial.getId(),
            historial.getPedido().getId(),
            historial.getEstado(),
            historial.getUsuario().getId(),
            historial.getFechaCambio(),
            historial.getObservacion()
        );
    }
}
