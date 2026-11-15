package pe.edu.cafeterialasrosas.service.impl;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cafeterialasrosas.dto.response.HistorialEstadoResponse;
import pe.edu.cafeterialasrosas.entity.EstadoPedido;
import pe.edu.cafeterialasrosas.entity.HistorialEstadoPedido;
import pe.edu.cafeterialasrosas.entity.Pedido;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.exception.OperationNotAllowedException;
import pe.edu.cafeterialasrosas.exception.ResourceNotFoundException;
import pe.edu.cafeterialasrosas.mapper.HistorialEstadoMapper;
import pe.edu.cafeterialasrosas.repository.HistorialEstadoPedidoRepository;
import pe.edu.cafeterialasrosas.repository.PedidoRepository;
import pe.edu.cafeterialasrosas.repository.UsuarioRepository;
import pe.edu.cafeterialasrosas.service.HistorialEstadoService;

@Service
@Transactional(readOnly = true)
public class HistorialEstadoServiceImpl implements HistorialEstadoService {

    private final HistorialEstadoPedidoRepository historialRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialEstadoMapper historialMapper;

    public HistorialEstadoServiceImpl(
        HistorialEstadoPedidoRepository historialRepository,
        PedidoRepository pedidoRepository,
        UsuarioRepository usuarioRepository,
        HistorialEstadoMapper historialMapper
    ) {
        this.historialRepository = historialRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialMapper = historialMapper;
    }

    @Override
    @Transactional
    public HistorialEstadoResponse registrarCambio(
        Pedido pedido,
        EstadoPedido estado,
        Usuario usuario,
        String observacion
    ) {
        HistorialEstadoPedido historial = new HistorialEstadoPedido();
        historial.setPedido(pedido);
        historial.setEstado(estado);
        historial.setUsuario(usuario);
        historial.setFechaCambio(Instant.now());
        historial.setObservacion(observacion);
        return historialMapper.toResponse(historialRepository.save(historial));
    }

    @Override
    public List<HistorialEstadoResponse> listarPorPedido(Long pedidoId, Usuario solicitante) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el pedido solicitado."));
        Usuario actor = cargarActor(solicitante);
        if (!AutorizacionOperativa.esPersonalOAdmin(actor)
            && !pedido.getUsuario().getId().equals(actor.getId())) {
            throw new OperationNotAllowedException("No tienes permiso para consultar este historial.");
        }
        return historialRepository.findByPedido_IdOrderByFechaCambioAscIdAsc(pedidoId)
            .stream()
            .map(historialMapper::toResponse)
            .toList();
    }

    private Usuario cargarActor(Usuario solicitante) {
        if (solicitante == null || solicitante.getId() == null) {
            throw new OperationNotAllowedException("Se requiere una cuenta activa para esta operación.");
        }
        Usuario actor = usuarioRepository.findById(solicitante.getId())
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario solicitado."));
        if (!actor.isActivo()) {
            throw new OperationNotAllowedException("La cuenta está inactiva.");
        }
        return actor;
    }
}
