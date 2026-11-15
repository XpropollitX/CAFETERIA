package pe.edu.cafeterialasrosas.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.cafeterialasrosas.dto.request.CrearPedidoRequest;
import pe.edu.cafeterialasrosas.dto.request.DetallePedidoRequest;
import pe.edu.cafeterialasrosas.entity.Categoria;
import pe.edu.cafeterialasrosas.entity.EstadoPago;
import pe.edu.cafeterialasrosas.entity.EstadoPedido;
import pe.edu.cafeterialasrosas.entity.Pedido;
import pe.edu.cafeterialasrosas.entity.Producto;
import pe.edu.cafeterialasrosas.entity.Rol;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.exception.BusinessRuleException;
import pe.edu.cafeterialasrosas.mapper.DetallePedidoMapper;
import pe.edu.cafeterialasrosas.mapper.PedidoMapper;
import pe.edu.cafeterialasrosas.repository.DetallePedidoRepository;
import pe.edu.cafeterialasrosas.repository.PagoRepository;
import pe.edu.cafeterialasrosas.repository.PedidoRepository;
import pe.edu.cafeterialasrosas.repository.ProductoRepository;
import pe.edu.cafeterialasrosas.repository.UsuarioRepository;
import pe.edu.cafeterialasrosas.service.HistorialEstadoService;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private DetallePedidoRepository detallePedidoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private HistorialEstadoService historialEstadoService;

    private PedidoServiceImpl pedidoService;
    private Usuario cliente;
    private Producto producto;

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoServiceImpl(
            pedidoRepository,
            detallePedidoRepository,
            productoRepository,
            usuarioRepository,
            pagoRepository,
            historialEstadoService,
            new PedidoMapper(new DetallePedidoMapper())
        );

        Rol rolCliente = new Rol();
        rolCliente.setNombre("CLIENTE");
        cliente = new Usuario();
        cliente.setId(12L);
        cliente.setActivo(true);
        cliente.setRol(rolCliente);

        Categoria categoria = new Categoria();
        categoria.setId(3L);
        categoria.setNombre("Bebidas");
        categoria.setActivo(true);
        producto = new Producto();
        producto.setId(5L);
        producto.setNombre("Café");
        producto.setCategoria(categoria);
        producto.setPrecio(new BigDecimal("3.50"));
        producto.setDisponible(true);
    }

    @Test
    void crearCalculaImportesYConservaElPrecioVigente() {
        when(usuarioRepository.findById(12L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(5L)).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            pedido.setId(20L);
            return pedido;
        });
        when(detallePedidoRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = pedidoService.crear(
            new CrearPedidoRequest(List.of(new DetallePedidoRequest(5L, 2)), "Sin azúcar"),
            cliente
        );

        assertThat(response.subtotal()).isEqualByComparingTo("7.00");
        assertThat(response.total()).isEqualByComparingTo("7.00");
        assertThat(response.estado()).isEqualTo(EstadoPedido.RECIBIDO);
        assertThat(response.detalles()).hasSize(1);
        assertThat(response.detalles().getFirst().precioUnitario()).isEqualByComparingTo("3.50");
        assertThat(response.detalles().getFirst().subtotal()).isEqualByComparingTo("7.00");
        verify(historialEstadoService).registrarCambio(
            any(Pedido.class),
            org.mockito.ArgumentMatchers.eq(EstadoPedido.RECIBIDO),
            org.mockito.ArgumentMatchers.eq(cliente),
            org.mockito.ArgumentMatchers.eq("Pedido recibido.")
        );
    }

    @Test
    void crearRechazaPedidoSinDetalles() {
        assertThatThrownBy(() -> pedidoService.crear(new CrearPedidoRequest(List.of(), null), cliente))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("al menos un producto");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void crearRechazaProductosNoDisponibles() {
        producto.setDisponible(false);
        when(usuarioRepository.findById(12L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(5L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> pedidoService.crear(
            new CrearPedidoRequest(List.of(new DetallePedidoRequest(5L, 1)), null),
            cliente
        ))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("no está disponible");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoNoIniciaPreparacionSiFaltaPago() {
        Rol rolPersonal = new Rol();
        rolPersonal.setNombre("PERSONAL");
        Usuario personal = new Usuario();
        personal.setId(40L);
        personal.setActivo(true);
        personal.setRol(rolPersonal);
        Pedido pedido = new Pedido();
        pedido.setId(20L);
        pedido.setEstado(EstadoPedido.RECIBIDO);
        pedido.setTotal(new BigDecimal("12.00"));
        when(usuarioRepository.findById(40L)).thenReturn(Optional.of(personal));
        when(pedidoRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(pedido));
        when(pagoRepository.sumMontoByPedidoAndEstado(20L, EstadoPago.PAGADO))
            .thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> pedidoService.cambiarEstado(
            20L,
            EstadoPedido.EN_PREPARACION,
            personal,
            null
        ))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("completamente pagado");

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(historialEstadoService, never()).registrarCambio(any(), any(), any(), any());
    }
}
