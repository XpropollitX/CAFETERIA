package pe.edu.cafeterialasrosas.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cafeterialasrosas.dto.response.DetallePedidoResponse;
import pe.edu.cafeterialasrosas.entity.Pedido;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.exception.OperationNotAllowedException;
import pe.edu.cafeterialasrosas.exception.ResourceNotFoundException;
import pe.edu.cafeterialasrosas.mapper.DetallePedidoMapper;
import pe.edu.cafeterialasrosas.repository.DetallePedidoRepository;
import pe.edu.cafeterialasrosas.repository.PedidoRepository;
import pe.edu.cafeterialasrosas.repository.UsuarioRepository;
import pe.edu.cafeterialasrosas.service.DetallePedidoService;

@Service
@Transactional(readOnly = true)
public class DetallePedidoServiceImpl implements DetallePedidoService {

    private final DetallePedidoRepository detallePedidoRepository;
    private final DetallePedidoMapper detallePedidoMapper;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;

    public DetallePedidoServiceImpl(
        DetallePedidoRepository detallePedidoRepository,
        DetallePedidoMapper detallePedidoMapper,
        PedidoRepository pedidoRepository,
        UsuarioRepository usuarioRepository
    ) {
        this.detallePedidoRepository = detallePedidoRepository;
        this.detallePedidoMapper = detallePedidoMapper;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<DetallePedidoResponse> listarPorPedido(Long pedidoId, Usuario solicitante) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el pedido solicitado."));
        Usuario actor = AutorizacionOperativa.cargarUsuarioActivo(solicitante, usuarioRepository);
        if (!AutorizacionOperativa.esPersonalOAdmin(actor)
            && !pedido.getUsuario().getId().equals(actor.getId())) {
            throw new OperationNotAllowedException("No tienes permiso para consultar este pedido.");
        }
        return detallePedidoRepository.findByPedido_IdOrderByIdAsc(pedidoId)
            .stream()
            .map(detallePedidoMapper::toResponse)
            .toList();
    }
}
