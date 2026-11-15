package pe.edu.cafeterialasrosas.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cafeterialasrosas.dto.request.CrearPedidoRequest;
import pe.edu.cafeterialasrosas.dto.request.DetallePedidoRequest;
import pe.edu.cafeterialasrosas.dto.response.PedidoResponse;
import pe.edu.cafeterialasrosas.entity.DetallePedido;
import pe.edu.cafeterialasrosas.entity.EstadoPago;
import pe.edu.cafeterialasrosas.entity.EstadoPedido;
import pe.edu.cafeterialasrosas.entity.Pedido;
import pe.edu.cafeterialasrosas.entity.Producto;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.exception.BusinessRuleException;
import pe.edu.cafeterialasrosas.exception.OperationNotAllowedException;
import pe.edu.cafeterialasrosas.exception.ResourceNotFoundException;
import pe.edu.cafeterialasrosas.mapper.PedidoMapper;
import pe.edu.cafeterialasrosas.repository.DetallePedidoRepository;
import pe.edu.cafeterialasrosas.repository.PagoRepository;
import pe.edu.cafeterialasrosas.repository.PedidoRepository;
import pe.edu.cafeterialasrosas.repository.ProductoRepository;
import pe.edu.cafeterialasrosas.repository.UsuarioRepository;
import pe.edu.cafeterialasrosas.service.HistorialEstadoService;
import pe.edu.cafeterialasrosas.service.PedidoService;

@Service
@Transactional(readOnly = true)
public class PedidoServiceImpl implements PedidoService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("99999999.99");
    private static final Set<EstadoPedido> ESTADOS_CANCELABLES = EnumSet.of(
        EstadoPedido.RECIBIDO,
        EstadoPedido.EN_PREPARACION,
        EstadoPedido.LISTO
    );
    private static final List<EstadoPedido> ESTADOS_OPERATIVOS = List.of(
        EstadoPedido.RECIBIDO,
        EstadoPedido.EN_PREPARACION,
        EstadoPedido.LISTO
    );

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagoRepository pagoRepository;
    private final HistorialEstadoService historialEstadoService;
    private final PedidoMapper pedidoMapper;

    public PedidoServiceImpl(
        PedidoRepository pedidoRepository,
        DetallePedidoRepository detallePedidoRepository,
        ProductoRepository productoRepository,
        UsuarioRepository usuarioRepository,
        PagoRepository pagoRepository,
        HistorialEstadoService historialEstadoService,
        PedidoMapper pedidoMapper
    ) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pagoRepository = pagoRepository;
        this.historialEstadoService = historialEstadoService;
        this.pedidoMapper = pedidoMapper;
    }

    @Override
    @Transactional
    public PedidoResponse crear(CrearPedidoRequest request, Usuario clienteSolicitante) {
        if (request == null || request.detalles() == null || request.detalles().isEmpty()) {
            throw new BusinessRuleException("El pedido debe contener al menos un producto.");
        }
        Usuario cliente = cargarActor(clienteSolicitante);
        if (!"CLIENTE".equals(cliente.getRol().getNombre())) {
            throw new OperationNotAllowedException("Solo una cuenta CLIENTE puede crear pedidos.");
        }

        Pedido pedido = new Pedido();
        pedido.setUsuario(cliente);
        pedido.setEstado(EstadoPedido.RECIBIDO);
        pedido.setObservaciones(request.observaciones());

        BigDecimal subtotalPedido = CERO;
        List<DetallePedido> detalles = new ArrayList<>();
        for (DetallePedidoRequest detalleRequest : request.detalles()) {
            if (detalleRequest == null || detalleRequest.cantidad() <= 0) {
                throw new BusinessRuleException("La cantidad de cada producto debe ser mayor que cero.");
            }
            if (detalleRequest.productoId() == null || detalleRequest.productoId() <= 0) {
                throw new BusinessRuleException("El identificador del producto debe ser válido.");
            }
            Producto producto = productoRepository.findById(detalleRequest.productoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No se encontró uno de los productos del pedido."
                ));
            if (!producto.isDisponible() || !producto.getCategoria().isActivo()) {
                throw new BusinessRuleException(
                    "Uno de los productos del pedido no está disponible."
                );
            }
            if (producto.getPrecio() == null || producto.getPrecio().signum() <= 0) {
                throw new BusinessRuleException(
                    "Uno de los productos tiene un precio inválido y no puede venderse."
                );
            }

            BigDecimal precioUnitario = producto.getPrecio().setScale(2, RoundingMode.UNNECESSARY);
            BigDecimal subtotalDetalle = precioUnitario
                .multiply(BigDecimal.valueOf(detalleRequest.cantidad()))
                .setScale(2, RoundingMode.UNNECESSARY);
            subtotalPedido = subtotalPedido.add(subtotalDetalle);
            if (subtotalPedido.compareTo(MONTO_MAXIMO) > 0) {
                throw new BusinessRuleException("El total del pedido supera el importe permitido.");
            }

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(detalleRequest.cantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotal(subtotalDetalle);
            detalles.add(detalle);
        }

        pedido.setSubtotal(subtotalPedido);
        pedido.setTotal(subtotalPedido);
        Pedido guardado = pedidoRepository.save(pedido);
        detalles.forEach(detalle -> detalle.setPedido(guardado));
        guardado.setDetalles(detallePedidoRepository.saveAll(detalles));
        historialEstadoService.registrarCambio(
            guardado,
            EstadoPedido.RECIBIDO,
            cliente,
            "Pedido recibido."
        );
        return pedidoMapper.toResponse(guardado);
    }

    @Override
    public PedidoResponse obtener(Long pedidoId, Usuario solicitante) {
        Pedido pedido = buscarPedido(pedidoId);
        Usuario actor = cargarActor(solicitante);
        validarAccesoPedido(pedido, actor);
        pedido.setDetalles(detallePedidoRepository.findByPedido_IdOrderByIdAsc(pedidoId));
        return pedidoMapper.toResponse(pedido);
    }

    @Override
    public List<PedidoResponse> listarPorUsuario(Usuario solicitante) {
        Usuario actor = cargarActor(solicitante);
        if (!"CLIENTE".equals(actor.getRol().getNombre())) {
            throw new OperationNotAllowedException(
                "La consulta de pedidos propios requiere una cuenta CLIENTE."
            );
        }
        return pedidoRepository.findByUsuario_IdOrderByFechaPedidoDesc(actor.getId())
            .stream()
            .map(this::mapearConDetalles)
            .toList();
    }

    @Override
    public List<PedidoResponse> listarOperativos(Usuario responsable) {
        Usuario actor = cargarActor(responsable);
        AutorizacionOperativa.requierePersonalOAdmin(actor);
        return pedidoRepository.findByEstadoInOrderByFechaPedidoAsc(ESTADOS_OPERATIVOS)
            .stream()
            .map(this::mapearConDetalles)
            .toList();
    }

    @Override
    public List<PedidoResponse> listarPorEstado(EstadoPedido estado, Usuario responsable) {
        if (estado == null) {
            throw new BusinessRuleException("El estado del pedido es obligatorio.");
        }
        Usuario actor = cargarActor(responsable);
        AutorizacionOperativa.requierePersonalOAdmin(actor);
        return pedidoRepository.findByEstadoOrderByFechaPedidoAsc(estado)
            .stream()
            .map(pedido -> mapearConDetalles(pedido))
            .toList();
    }

    @Override
    @Transactional
    public PedidoResponse cambiarEstado(
        Long pedidoId,
        EstadoPedido nuevoEstado,
        Usuario responsableSolicitante,
        String observacion
    ) {
        Usuario responsable = cargarActor(responsableSolicitante);
        AutorizacionOperativa.requierePersonalOAdmin(responsable);
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el pedido solicitado."));

        validarTransicion(pedido.getEstado(), nuevoEstado, observacion);
        if (nuevoEstado == EstadoPedido.EN_PREPARACION) {
            BigDecimal totalPagado = pagoRepository.sumMontoByPedidoAndEstado(
                pedidoId,
                EstadoPago.PAGADO
            );
            if (totalPagado.compareTo(pedido.getTotal()) < 0) {
                throw new BusinessRuleException(
                    "El pedido no puede pasar a preparación hasta que esté completamente pagado."
                );
            }
        }

        pedido.setEstado(nuevoEstado);
        pedidoRepository.save(pedido);
        historialEstadoService.registrarCambio(pedido, nuevoEstado, responsable, observacion);
        pedido.setDetalles(detallePedidoRepository.findByPedido_IdOrderByIdAsc(pedidoId));
        return pedidoMapper.toResponse(pedido);
    }

    private void validarTransicion(EstadoPedido actual, EstadoPedido destino, String observacion) {
        if (destino == null) {
            throw new BusinessRuleException("El nuevo estado del pedido es obligatorio.");
        }
        boolean transicionPermitida = switch (actual) {
            case RECIBIDO -> destino == EstadoPedido.EN_PREPARACION || destino == EstadoPedido.CANCELADO;
            case EN_PREPARACION -> destino == EstadoPedido.LISTO || destino == EstadoPedido.CANCELADO;
            case LISTO -> destino == EstadoPedido.ENTREGADO || destino == EstadoPedido.CANCELADO;
            case ENTREGADO, CANCELADO -> false;
        };
        if (!transicionPermitida) {
            throw new BusinessRuleException("La transición de estado solicitada no está permitida.");
        }
        if (destino == EstadoPedido.CANCELADO) {
            if (!ESTADOS_CANCELABLES.contains(actual)) {
                throw new BusinessRuleException("El pedido ya no puede cancelarse.");
            }
            if (observacion == null || observacion.isBlank()) {
                throw new BusinessRuleException("Se debe registrar el motivo de la cancelación.");
            }
            if (observacion.length() > 1000) {
                throw new BusinessRuleException("El motivo de cancelación no puede superar 1000 caracteres.");
            }
        }
    }

    private void validarAccesoPedido(Pedido pedido, Usuario actor) {
        if (!AutorizacionOperativa.esPersonalOAdmin(actor)
            && !pedido.getUsuario().getId().equals(actor.getId())) {
            throw new OperationNotAllowedException("No tienes permiso para consultar este pedido.");
        }
    }

    private PedidoResponse mapearConDetalles(Pedido pedido) {
        pedido.setDetalles(detallePedidoRepository.findByPedido_IdOrderByIdAsc(pedido.getId()));
        return pedidoMapper.toResponse(pedido);
    }

    private Pedido buscarPedido(Long pedidoId) {
        return pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el pedido solicitado."));
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
