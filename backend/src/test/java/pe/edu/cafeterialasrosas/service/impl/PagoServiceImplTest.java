package pe.edu.cafeterialasrosas.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.cafeterialasrosas.dto.request.RegistrarPagoRequest;
import pe.edu.cafeterialasrosas.entity.EstadoPago;
import pe.edu.cafeterialasrosas.entity.EstadoPedido;
import pe.edu.cafeterialasrosas.entity.MetodoPago;
import pe.edu.cafeterialasrosas.entity.Pago;
import pe.edu.cafeterialasrosas.entity.Pedido;
import pe.edu.cafeterialasrosas.entity.Rol;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.exception.BusinessRuleException;
import pe.edu.cafeterialasrosas.mapper.PagoMapper;
import pe.edu.cafeterialasrosas.repository.PagoRepository;
import pe.edu.cafeterialasrosas.repository.PedidoRepository;
import pe.edu.cafeterialasrosas.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private PagoServiceImpl pagoService;
    private Usuario personal;
    private Usuario cliente;
    private Pedido pedido;
    private Pago pago;

    @BeforeEach
    void setUp() {
        pagoService = new PagoServiceImpl(
            pagoRepository,
            pedidoRepository,
            usuarioRepository,
            new PagoMapper()
        );
        Rol rolPersonal = new Rol();
        rolPersonal.setNombre("PERSONAL");
        personal = new Usuario();
        personal.setId(40L);
        personal.setActivo(true);
        personal.setRol(rolPersonal);

        Rol rolCliente = new Rol();
        rolCliente.setNombre("CLIENTE");
        cliente = new Usuario();
        cliente.setId(12L);
        cliente.setActivo(true);
        cliente.setRol(rolCliente);

        pedido = new Pedido();
        pedido.setId(20L);
        pedido.setUsuario(cliente);
        pedido.setEstado(EstadoPedido.RECIBIDO);
        pedido.setTotal(new BigDecimal("10.00"));

        pago = new Pago();
        pago.setId(30L);
        pago.setPedido(pedido);
        pago.setMetodoPago(MetodoPago.EFECTIVO);
        pago.setMonto(new BigDecimal("3.00"));
        pago.setEstadoPago(EstadoPago.PENDIENTE);
    }

    @Test
    void registrarIntencionNoPermiteQuePagosPendientesExcedanElTotal() {
        when(usuarioRepository.findById(12L)).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(pedido));
        when(pagoRepository.sumMontoByPedidoAndEstado(20L, EstadoPago.PAGADO))
            .thenReturn(new BigDecimal("4.00"));
        when(pagoRepository.sumMontoByPedidoAndEstado(20L, EstadoPago.PENDIENTE))
            .thenReturn(new BigDecimal("5.00"));

        assertThatThrownBy(() -> pagoService.registrarIntencion(
            20L,
            cliente,
            new RegistrarPagoRequest(MetodoPago.YAPE, new BigDecimal("2.00"), null)
        ))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("saldo pendiente");

        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void verificarRechazaUnPagoQueSuperariaElTotalConfirmado() {
        when(usuarioRepository.findById(40L)).thenReturn(Optional.of(personal));
        when(pagoRepository.findById(30L)).thenReturn(Optional.of(pago));
        when(pedidoRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(pedido));
        when(pagoRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(pago));
        when(pagoRepository.sumMontoByPedidoAndEstado(20L, EstadoPago.PAGADO))
            .thenReturn(new BigDecimal("8.00"));

        assertThatThrownBy(() -> pagoService.verificar(30L, personal))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("superaría el total");

        assertThat(pago.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void verificarRegistraAlPersonalYLaFechaDelPago() {
        when(usuarioRepository.findById(40L)).thenReturn(Optional.of(personal));
        when(pagoRepository.findById(30L)).thenReturn(Optional.of(pago));
        when(pedidoRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(pedido));
        when(pagoRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(pago));
        when(pagoRepository.sumMontoByPedidoAndEstado(20L, EstadoPago.PAGADO))
            .thenReturn(new BigDecimal("7.00"));
        when(pagoRepository.save(pago)).thenReturn(pago);

        var response = pagoService.verificar(30L, personal);

        assertThat(response.estadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(response.registradoPorId()).isEqualTo(40L);
        assertThat(response.fechaPago()).isNotNull();
    }
}
