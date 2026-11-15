package pe.edu.cafeterialasrosas.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cafeterialasrosas.dto.request.RegistrarPagoRequest;
import pe.edu.cafeterialasrosas.dto.response.PagoResponse;
import pe.edu.cafeterialasrosas.entity.EstadoPago;
import pe.edu.cafeterialasrosas.entity.EstadoPedido;
import pe.edu.cafeterialasrosas.entity.Pago;
import pe.edu.cafeterialasrosas.entity.Pedido;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.exception.BusinessRuleException;
import pe.edu.cafeterialasrosas.exception.OperationNotAllowedException;
import pe.edu.cafeterialasrosas.exception.ResourceNotFoundException;
import pe.edu.cafeterialasrosas.mapper.PagoMapper;
import pe.edu.cafeterialasrosas.repository.PagoRepository;
import pe.edu.cafeterialasrosas.repository.PedidoRepository;
import pe.edu.cafeterialasrosas.repository.UsuarioRepository;
import pe.edu.cafeterialasrosas.service.PagoService;

@Service
@Transactional(readOnly = true)
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagoMapper pagoMapper;

    public PagoServiceImpl(
        PagoRepository pagoRepository,
        PedidoRepository pedidoRepository,
        UsuarioRepository usuarioRepository,
        PagoMapper pagoMapper
    ) {
        this.pagoRepository = pagoRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pagoMapper = pagoMapper;
    }

    @Override
    @Transactional
    public PagoResponse registrarIntencion(
        Long pedidoId,
        Usuario clienteSolicitante,
        RegistrarPagoRequest request
    ) {
        if (request == null || request.metodoPago() == null || request.monto() == null
            || request.monto().signum() <= 0 || request.monto().scale() > 2
            || request.monto().compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new BusinessRuleException("Los datos del pago son inválidos.");
        }
        Usuario cliente = cargarActor(clienteSolicitante);
        if (!"CLIENTE".equals(cliente.getRol().getNombre())) {
            throw new OperationNotAllowedException("Solo una cuenta CLIENTE puede registrar una intención de pago.");
        }
        Pedido pedido = bloquearPedido(pedidoId);
        if (!pedido.getUsuario().getId().equals(cliente.getId())) {
            throw new OperationNotAllowedException("No tienes permiso para registrar pagos de este pedido.");
        }
        if (pedido.getEstado() == EstadoPedido.CANCELADO || pedido.getEstado() == EstadoPedido.ENTREGADO) {
            throw new BusinessRuleException("No se pueden registrar pagos para un pedido cerrado.");
        }

        BigDecimal pagado = pagoRepository.sumMontoByPedidoAndEstado(pedidoId, EstadoPago.PAGADO);
        BigDecimal pendiente = pagoRepository.sumMontoByPedidoAndEstado(pedidoId, EstadoPago.PENDIENTE);
        BigDecimal saldoDisponible = pedido.getTotal().subtract(pagado).subtract(pendiente);
        if (request.monto().compareTo(saldoDisponible) > 0) {
            throw new BusinessRuleException(
                "El monto supera el saldo pendiente del pedido, considerando pagos aún por verificar."
            );
        }

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodoPago(request.metodoPago());
        pago.setMonto(request.monto());
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pago.setReferencia(request.referencia());
        return pagoMapper.toResponse(pagoRepository.save(pago));
    }

    @Override
    public List<PagoResponse> listarPorPedido(Long pedidoId, Usuario solicitante) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el pedido solicitado."));
        Usuario actor = cargarActor(solicitante);
        if (!AutorizacionOperativa.esPersonalOAdmin(actor)
            && !pedido.getUsuario().getId().equals(actor.getId())) {
            throw new OperationNotAllowedException("No tienes permiso para consultar los pagos de este pedido.");
        }
        return pagoRepository.findByPedido_IdOrderByIdAsc(pedidoId)
            .stream()
            .map(pagoMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public PagoResponse verificar(Long pagoId, Usuario verificadorSolicitante) {
        Usuario verificador = cargarActor(verificadorSolicitante);
        AutorizacionOperativa.requierePersonalOAdmin(verificador);
        Pago referencia = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el pago solicitado."));

        Pedido pedido = bloquearPedido(referencia.getPedido().getId());
        Pago pago = pagoRepository.findByIdForUpdate(pagoId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el pago solicitado."));
        if (pago.getEstadoPago() != EstadoPago.PENDIENTE) {
            throw new BusinessRuleException("Solo se pueden verificar pagos pendientes.");
        }
        if (pedido.getEstado() == EstadoPedido.CANCELADO || pedido.getEstado() == EstadoPedido.ENTREGADO) {
            throw new BusinessRuleException("No se pueden verificar pagos de un pedido cerrado.");
        }

        BigDecimal yaPagado = pagoRepository.sumMontoByPedidoAndEstado(
            pedido.getId(),
            EstadoPago.PAGADO
        );
        if (yaPagado.add(pago.getMonto()).compareTo(pedido.getTotal()) > 0) {
            throw new BusinessRuleException("La verificación superaría el total del pedido.");
        }

        pago.setEstadoPago(EstadoPago.PAGADO);
        pago.setRegistradoPor(verificador);
        pago.setFechaPago(Instant.now());
        return pagoMapper.toResponse(pagoRepository.save(pago));
    }

    @Override
    @Transactional
    public PagoResponse anular(Long pagoId, Usuario responsableSolicitante) {
        Usuario responsable = cargarActor(responsableSolicitante);
        AutorizacionOperativa.requierePersonalOAdmin(responsable);
        Pago referencia = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el pago solicitado."));

        bloquearPedido(referencia.getPedido().getId());
        Pago pago = pagoRepository.findByIdForUpdate(pagoId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el pago solicitado."));
        if (pago.getEstadoPago() != EstadoPago.PENDIENTE) {
            throw new BusinessRuleException("Solo se pueden anular pagos pendientes.");
        }
        pago.setEstadoPago(EstadoPago.ANULADO);
        return pagoMapper.toResponse(pagoRepository.save(pago));
    }

    private Pedido bloquearPedido(Long pedidoId) {
        return pedidoRepository.findByIdForUpdate(pedidoId)
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
