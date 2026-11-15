package pe.edu.cafeterialasrosas.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import pe.edu.cafeterialasrosas.TestCredentials;
import pe.edu.cafeterialasrosas.entity.Categoria;
import pe.edu.cafeterialasrosas.entity.EstadoPedido;
import pe.edu.cafeterialasrosas.entity.HistorialEstadoPedido;
import pe.edu.cafeterialasrosas.entity.EstadoPago;
import pe.edu.cafeterialasrosas.entity.Pago;
import pe.edu.cafeterialasrosas.entity.Producto;
import pe.edu.cafeterialasrosas.entity.Pedido;
import pe.edu.cafeterialasrosas.entity.Rol;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.repository.CategoriaRepository;
import pe.edu.cafeterialasrosas.repository.HistorialEstadoPedidoRepository;
import pe.edu.cafeterialasrosas.repository.PagoRepository;
import pe.edu.cafeterialasrosas.repository.PedidoRepository;
import pe.edu.cafeterialasrosas.repository.ProductoRepository;
import pe.edu.cafeterialasrosas.repository.RolRepository;
import pe.edu.cafeterialasrosas.repository.UsuarioRepository;
import pe.edu.cafeterialasrosas.service.HistorialEstadoService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PedidoControllerIntegrationTest {

    private static final String JWT_SECRET = generateSecret();
    private static final String PASSWORD = "pedido-test-password";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> JWT_SECRET);
        registry.add("app.initial-admin.password", () -> TestCredentials.INITIAL_ADMIN_PASSWORD);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private HistorialEstadoPedidoRepository historialRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @SpyBean
    private HistorialEstadoService historialEstadoService;

    @Test
    void createsMultiProductOrderFromDatabasePricesAndKeepsHistoricalPrices() throws Exception {
        Usuario cliente = crearUsuario("CLIENTE");
        Cookie authCookie = iniciarSesion(cliente.getCorreo());
        Categoria categoria = crearCategoria();
        Producto cafe = crearProducto(categoria, "Café", "3.50", true);
        Producto te = crearProducto(categoria, "Té", "2.25", true);
        Csrf csrf = obtenerCsrf();

        MvcResult result = mockMvc.perform(crearPedidoRequest(
                authCookie,
                csrf,
                """
                    {
                      "detalles":[
                        {"productoId":%d,"cantidad":2},
                        {"productoId":%d,"cantidad":1}
                      ],
                      "observaciones":"Sin azúcar",
                      "usuarioId":999999,
                      "subtotal":0.01,
                      "total":0.01,
                      "estado":"ENTREGADO"
                    }
                    """.formatted(cafe.getId(), te.getId())
            ))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.usuarioId").value(cliente.getId()))
            .andExpect(jsonPath("$.estado").value("RECIBIDO"))
            .andExpect(jsonPath("$.subtotal").value(9.25))
            .andExpect(jsonPath("$.total").value(9.25))
            .andExpect(jsonPath("$.detalles.length()").value(2))
            .andReturn();

        long pedidoId = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("id").asLong();
        List<HistorialEstadoPedido> history =
            historialRepository.findByPedido_IdOrderByFechaCambioAscIdAsc(pedidoId);
        org.assertj.core.api.Assertions.assertThat(history).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(history.getFirst().getEstado())
            .isEqualTo(EstadoPedido.RECIBIDO);
        org.assertj.core.api.Assertions.assertThat(history.getFirst().getUsuario().getId())
            .isEqualTo(cliente.getId());

        cafe.setPrecio(new BigDecimal("99.00"));
        productoRepository.saveAndFlush(cafe);
        mockMvc.perform(get("/api/pedidos/" + pedidoId).cookie(authCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.detalles[0].precioUnitario").value(3.50))
            .andExpect(jsonPath("$.detalles[0].subtotal").value(7.00));
    }

    @Test
    void rejectsEmptyOrdersInvalidQuantitiesAndUnavailableProducts() throws Exception {
        Usuario cliente = crearUsuario("CLIENTE");
        Cookie authCookie = iniciarSesion(cliente.getCorreo());
        Categoria categoria = crearCategoria();
        Producto unavailable = crearProducto(categoria, "No disponible", "4.00", false);

        Csrf csrf = obtenerCsrf();
        mockMvc.perform(crearPedidoRequest(
                authCookie,
                csrf,
                "{\"detalles\":[],\"observaciones\":null}"
            ))
            .andExpect(status().isBadRequest());

        csrf = obtenerCsrf();
        mockMvc.perform(crearPedidoRequest(
                authCookie,
                csrf,
                """
                    {"detalles":[{"productoId":%d,"cantidad":0}]}
                    """.formatted(unavailable.getId())
            ))
            .andExpect(status().isBadRequest());

        csrf = obtenerCsrf();
        mockMvc.perform(crearPedidoRequest(
                authCookie,
                csrf,
                """
                    {"detalles":[{"productoId":%d,"cantidad":1}]}
                    """.formatted(unavailable.getId())
            ))
            .andExpect(status().isUnprocessableEntity());

        org.assertj.core.api.Assertions.assertThat(
            pedidoRepository.findByUsuario_IdOrderByFechaPedidoDesc(cliente.getId())
        ).isEmpty();
    }

    @Test
    void clientsCanOnlyReadTheirOwnOrdersAndOnlyClientsCanCreateOrders() throws Exception {
        Usuario owner = crearUsuario("CLIENTE");
        Usuario otherClient = crearUsuario("CLIENTE");
        Cookie ownerCookie = iniciarSesion(owner.getCorreo());
        Categoria categoria = crearCategoria();
        Producto product = crearProducto(categoria, "Producto", "5.00", true);
        Csrf csrf = obtenerCsrf();

        MvcResult created = mockMvc.perform(crearPedidoRequest(
                ownerCookie,
                csrf,
                """
                    {"detalles":[{"productoId":%d,"cantidad":1}]}
                    """.formatted(product.getId())
            ))
            .andExpect(status().isCreated())
            .andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/pedidos/mis-pedidos").cookie(ownerCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].usuarioId").value(owner.getId()));

        Cookie otherCookie = iniciarSesion(otherClient.getCorreo());
        mockMvc.perform(get("/api/pedidos/" + orderId).cookie(otherCookie))
            .andExpect(status().isForbidden());

        Usuario personal = crearUsuario("PERSONAL");
        Cookie personalCookie = iniciarSesion(personal.getCorreo());
        Csrf personalCsrf = obtenerCsrf();
        mockMvc.perform(crearPedidoRequest(
                personalCookie,
                personalCsrf,
                """
                    {"detalles":[{"productoId":%d,"cantidad":1}]}
                    """.formatted(product.getId())
            ))
            .andExpect(status().isForbidden());
    }

    @Test
    void registersOnlySupportedMethodsAndReservesSplitPaymentBalance() throws Exception {
        Usuario cliente = crearUsuario("CLIENTE");
        Cookie clienteCookie = iniciarSesion(cliente.getCorreo());
        long orderId = crearPedido(clienteCookie, "50.00");

        registrarPago(clienteCookie, orderId, "EFECTIVO", "15.00")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estadoPago").value("PENDIENTE"));
        registrarPago(clienteCookie, orderId, "YAPE", "20.00")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.metodoPago").value("YAPE"));
        registrarPago(clienteCookie, orderId, "TRANSFERENCIA", "15.00")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.metodoPago").value("TRANSFERENCIA"));

        mockMvc.perform(get("/api/pedidos/" + orderId + "/pagos").cookie(clienteCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));

        registrarPago(clienteCookie, orderId, "EFECTIVO", "0.01")
            .andExpect(status().isUnprocessableEntity());
        registrarPago(clienteCookie, orderId, "PLIN", "1.00")
            .andExpect(status().isBadRequest());
        org.assertj.core.api.Assertions.assertThat(pagoRepository.findByPedido_IdOrderByIdAsc(orderId))
            .hasSize(3)
            .allMatch(pago -> pago.getEstadoPago() == EstadoPago.PENDIENTE);
    }

    @Test
    void staffVerifiesOnceWithoutChangingOrderStateAndAnnulledPaymentsDoNotReserveBalance() throws Exception {
        Usuario cliente = crearUsuario("CLIENTE");
        Cookie clienteCookie = iniciarSesion(cliente.getCorreo());
        long orderId = crearPedido(clienteCookie, "10.00");
        MvcResult pendingResult = registrarPago(clienteCookie, orderId, "EFECTIVO", "10.00")
            .andExpect(status().isCreated())
            .andReturn();
        long paymentId = objectMapper.readTree(pendingResult.getResponse().getContentAsString())
            .get("id").asLong();

        Usuario personal = crearUsuario("PERSONAL");
        Cookie personalCookie = iniciarSesion(personal.getCorreo());
        Csrf personalCsrf = obtenerCsrf();
        Csrf clienteCsrf = obtenerCsrf();

        mockMvc.perform(patch("/api/pagos/" + paymentId + "/verificar")
                .cookie(clienteCookie, clienteCsrf.cookie())
                .header(clienteCsrf.headerName(), clienteCsrf.token()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/pedidos/" + orderId).cookie(clienteCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RECIBIDO"));

        mockMvc.perform(patch("/api/pagos/" + paymentId + "/verificar")
                .cookie(personalCookie, personalCsrf.cookie())
                .header(personalCsrf.headerName(), personalCsrf.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estadoPago").value("PAGADO"))
            .andExpect(jsonPath("$.registradoPorId").value(personal.getId()));

        mockMvc.perform(get("/api/pedidos/" + orderId).cookie(clienteCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RECIBIDO"));

        Csrf retryCsrf = obtenerCsrf();
        mockMvc.perform(patch("/api/pagos/" + paymentId + "/verificar")
                .cookie(personalCookie, retryCsrf.cookie())
                .header(retryCsrf.headerName(), retryCsrf.token()))
            .andExpect(status().isUnprocessableEntity());

        long secondOrderId = crearPedido(clienteCookie, "10.00");
        MvcResult secondPayment = registrarPago(clienteCookie, secondOrderId, "YAPE", "4.00")
            .andExpect(status().isCreated())
            .andReturn();
        long secondPaymentId = objectMapper.readTree(secondPayment.getResponse().getContentAsString())
            .get("id").asLong();
        Csrf annulCsrf = obtenerCsrf();
        mockMvc.perform(patch("/api/pagos/" + secondPaymentId + "/anular")
                .cookie(personalCookie, annulCsrf.cookie())
                .header(annulCsrf.headerName(), annulCsrf.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estadoPago").value("ANULADO"));

        registrarPago(clienteCookie, secondOrderId, "TRANSFERENCIA", "10.00")
            .andExpect(status().isCreated());
        Pago annulled = pagoRepository.findById(secondPaymentId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(annulled.getEstadoPago()).isEqualTo(EstadoPago.ANULADO);
    }

    @Test
    void clientCannotRegisterPaymentForAnotherClientsOrder() throws Exception {
        Usuario owner = crearUsuario("CLIENTE");
        Usuario attacker = crearUsuario("CLIENTE");
        Cookie ownerCookie = iniciarSesion(owner.getCorreo());
        long orderId = crearPedido(ownerCookie, "10.00");
        Cookie attackerCookie = iniciarSesion(attacker.getCorreo());

        registrarPago(attackerCookie, orderId, "YAPE", "1.00")
            .andExpect(status().isForbidden());
    }

    @Test
    void operationalOrdersRequireStaffAndPreparationRequiresAllPaymentsVerified() throws Exception {
        Usuario cliente = crearUsuario("CLIENTE");
        Usuario otroCliente = crearUsuario("CLIENTE");
        Usuario personal = crearUsuario("PERSONAL");
        Usuario admin = crearUsuario("ADMIN");
        Cookie clienteCookie = iniciarSesion(cliente.getCorreo());
        Cookie personalCookie = iniciarSesion(personal.getCorreo());
        Cookie adminCookie = iniciarSesion(admin.getCorreo());
        long orderId = crearPedido(clienteCookie, "50.00");

        mockMvc.perform(get("/api/pedidos/operativos").cookie(clienteCookie))
            .andExpect(status().isForbidden());
        MvcResult operationalResult = mockMvc.perform(get("/api/pedidos/operativos").cookie(personalCookie))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode operationalOrders = objectMapper.readTree(operationalResult.getResponse().getContentAsString());
        JsonNode operationalOrder = java.util.stream.StreamSupport.stream(
                operationalOrders.spliterator(),
                false
            )
            .filter(order -> order.path("id").asLong() == orderId)
            .findFirst()
            .orElseThrow();
        org.assertj.core.api.Assertions.assertThat(operationalOrder.path("estado").asText())
            .isEqualTo("RECIBIDO");
        org.assertj.core.api.Assertions.assertThat(operationalOrder.path("detalles").size()).isEqualTo(1);

        mockMvc.perform(get("/api/pedidos/" + orderId + "/historial").cookie(clienteCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].estado").value("RECIBIDO"));
        Cookie otroClienteCookie = iniciarSesion(otroCliente.getCorreo());
        mockMvc.perform(get("/api/pedidos/" + orderId + "/historial").cookie(otroClienteCookie))
            .andExpect(status().isForbidden());

        Csrf clienteCsrf = obtenerCsrf();
        mockMvc.perform(patch("/api/pedidos/" + orderId + "/estado")
                .cookie(clienteCookie, clienteCsrf.cookie())
                .header(clienteCsrf.headerName(), clienteCsrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"EN_PREPARACION\"}"))
            .andExpect(status().isForbidden());

        Csrf personalCsrf = obtenerCsrf();
        mockMvc.perform(patch("/api/pedidos/" + orderId + "/estado")
                .cookie(personalCookie, personalCsrf.cookie())
                .header(personalCsrf.headerName(), personalCsrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"LISTO\"}"))
            .andExpect(status().isUnprocessableEntity());

        registrarPago(clienteCookie, orderId, "YAPE", "20.00").andExpect(status().isCreated());
        registrarPago(clienteCookie, orderId, "TRANSFERENCIA", "30.00").andExpect(status().isCreated());
        long[] paymentIds = pagoRepository.findByPedido_IdOrderByIdAsc(orderId).stream()
            .mapToLong(Pago::getId)
            .toArray();
        Csrf verifyFirst = obtenerCsrf();
        mockMvc.perform(patch("/api/pagos/" + paymentIds[0] + "/verificar")
                .cookie(personalCookie, verifyFirst.cookie())
                .header(verifyFirst.headerName(), verifyFirst.token()))
            .andExpect(status().isOk());

        Csrf partialPayment = obtenerCsrf();
        mockMvc.perform(patch("/api/pedidos/" + orderId + "/estado")
                .cookie(personalCookie, partialPayment.cookie())
                .header(partialPayment.headerName(), partialPayment.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"EN_PREPARACION\"}"))
            .andExpect(status().isUnprocessableEntity());

        Csrf verifySecond = obtenerCsrf();
        mockMvc.perform(patch("/api/pagos/" + paymentIds[1] + "/verificar")
                .cookie(personalCookie, verifySecond.cookie())
                .header(verifySecond.headerName(), verifySecond.token()))
            .andExpect(status().isOk());

        Csrf startPreparation = obtenerCsrf();
        mockMvc.perform(patch("/api/pedidos/" + orderId + "/estado")
                .cookie(personalCookie, startPreparation.cookie())
                .header(startPreparation.headerName(), startPreparation.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"EN_PREPARACION\",\"observacion\":\"Inicio\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("EN_PREPARACION"));

        Csrf markReady = obtenerCsrf();
        mockMvc.perform(patch("/api/pedidos/" + orderId + "/estado")
                .cookie(adminCookie, markReady.cookie())
                .header(markReady.headerName(), markReady.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"LISTO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("LISTO"));

        mockMvc.perform(get("/api/pedidos/" + orderId + "/historial").cookie(adminCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[1].usuarioId").value(personal.getId()))
            .andExpect(jsonPath("$[2].usuarioId").value(admin.getId()));
        org.assertj.core.api.Assertions.assertThat(
            historialRepository.findByPedido_IdOrderByFechaCambioAscIdAsc(orderId)
        ).extracting(HistorialEstadoPedido::getEstado)
            .containsExactly(
                EstadoPedido.RECIBIDO,
                EstadoPedido.EN_PREPARACION,
                EstadoPedido.LISTO
            );
    }

    @Test
    void cancellationRecordsReasonAndResponsibleUserWithoutDeletingOrderDetailsOrPayments() throws Exception {
        Usuario cliente = crearUsuario("CLIENTE");
        Usuario personal = crearUsuario("PERSONAL");
        Cookie clienteCookie = iniciarSesion(cliente.getCorreo());
        Cookie personalCookie = iniciarSesion(personal.getCorreo());
        long orderId = crearPedido(clienteCookie, "10.00");
        registrarPago(clienteCookie, orderId, "EFECTIVO", "4.00").andExpect(status().isCreated());

        Csrf csrf = obtenerCsrf();
        mockMvc.perform(patch("/api/pedidos/" + orderId + "/cancelar")
                .cookie(personalCookie, csrf.cookie())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacion\":\"Cliente solicitó cancelar\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADO"))
            .andExpect(jsonPath("$.detalles.length()").value(1));

        List<HistorialEstadoPedido> history =
            historialRepository.findByPedido_IdOrderByFechaCambioAscIdAsc(orderId);
        org.assertj.core.api.Assertions.assertThat(history).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(history.get(1).getUsuario().getId())
            .isEqualTo(personal.getId());
        org.assertj.core.api.Assertions.assertThat(history.get(1).getObservacion())
            .isEqualTo("Cliente solicitó cancelar");
        org.assertj.core.api.Assertions.assertThat(pagoRepository.findByPedido_IdOrderByIdAsc(orderId))
            .hasSize(1)
            .allMatch(pago -> pago.getEstadoPago() == EstadoPago.PENDIENTE);

        Csrf retry = obtenerCsrf();
        mockMvc.perform(patch("/api/pedidos/" + orderId + "/estado")
                .cookie(personalCookie, retry.cookie())
                .header(retry.headerName(), retry.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"EN_PREPARACION\"}"))
            .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/pedidos/" + orderId).cookie(clienteCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.detalles.length()").value(1));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void statusAndHistoryAreRolledBackTogetherWhenHistoryInsertFails() throws Exception {
        Usuario cliente = crearUsuario("CLIENTE");
        Usuario personal = crearUsuario("PERSONAL");
        Cookie clienteCookie = iniciarSesion(cliente.getCorreo());
        Cookie personalCookie = iniciarSesion(personal.getCorreo());
        long orderId = crearPedido(clienteCookie, "10.00");
        long paymentId = objectMapper.readTree(
            registrarPago(clienteCookie, orderId, "EFECTIVO", "10.00")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
        ).get("id").asLong();
        Csrf paymentCsrf = obtenerCsrf();
        mockMvc.perform(patch("/api/pagos/" + paymentId + "/verificar")
                .cookie(personalCookie, paymentCsrf.cookie())
                .header(paymentCsrf.headerName(), paymentCsrf.token()))
            .andExpect(status().isOk());

        doThrow(new IllegalStateException("Fallo de prueba al guardar historial"))
            .when(historialEstadoService)
            .registrarCambio(
                org.mockito.ArgumentMatchers.any(Pedido.class),
                eq(EstadoPedido.EN_PREPARACION),
                org.mockito.ArgumentMatchers.any(Usuario.class),
                eq("Inicio")
            );

        Csrf csrf = obtenerCsrf();
        mockMvc.perform(patch("/api/pedidos/" + orderId + "/estado")
                .cookie(personalCookie, csrf.cookie())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"EN_PREPARACION\",\"observacion\":\"Inicio\"}"))
            .andExpect(status().isInternalServerError());

        mockMvc.perform(get("/api/pedidos/" + orderId).cookie(clienteCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RECIBIDO"));
        mockMvc.perform(get("/api/pedidos/" + orderId + "/historial").cookie(clienteCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    private Usuario crearUsuario(String roleName) {
        Rol role = rolRepository.findByNombre(roleName).orElseThrow();
        Usuario usuario = new Usuario();
        usuario.setNombres("Usuario");
        usuario.setApellidos("Pedido");
        usuario.setCorreo("pedido-" + UUID.randomUUID() + "@example.test");
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(role);
        usuario.setActivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Categoria crearCategoria() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Categoria " + UUID.randomUUID());
        categoria.setActivo(true);
        return categoriaRepository.saveAndFlush(categoria);
    }

    private Producto crearProducto(Categoria categoria, String nombre, String precio, boolean disponible) {
        Producto producto = new Producto();
        producto.setCategoria(categoria);
        producto.setNombre(nombre);
        producto.setPrecio(new BigDecimal(precio));
        producto.setDisponible(disponible);
        return productoRepository.saveAndFlush(producto);
    }

    private Cookie iniciarSesion(String correo) throws Exception {
        Csrf csrf = obtenerCsrf();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .cookie(csrf.cookie())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"correo":"%s","password":"%s"}
                    """.formatted(correo, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();
        return result.getResponse().getCookie("AUTH_TOKEN");
    }

    private Csrf obtenerCsrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Csrf(
            result.getResponse().getCookie("XSRF-TOKEN"),
            body.get("headerName").asText(),
            body.get("token").asText()
        );
    }

    private MockHttpServletRequestBuilder crearPedidoRequest(Cookie authCookie, Csrf csrf, String body) {
        return post("/api/pedidos")
            .cookie(authCookie, csrf.cookie())
            .header(csrf.headerName(), csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);
    }

    private long crearPedido(Cookie authCookie, String totalEsperado) throws Exception {
        Categoria categoria = crearCategoria();
        Producto producto = crearProducto(categoria, "Producto", totalEsperado, true);
        MvcResult result = mockMvc.perform(crearPedidoRequest(
                authCookie,
                obtenerCsrf(),
                """
                    {"detalles":[{"productoId":%d,"cantidad":1}]}
                    """.formatted(producto.getId())
            ))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total").value(new BigDecimal(totalEsperado).doubleValue()))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions registrarPago(
        Cookie authCookie,
        long pedidoId,
        String metodo,
        String monto
    ) throws Exception {
        Csrf csrf = obtenerCsrf();
        return mockMvc.perform(post("/api/pedidos/" + pedidoId + "/pagos")
            .cookie(authCookie, csrf.cookie())
            .header(csrf.headerName(), csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"metodoPago":"%s","monto":%s,"referencia":null}
                """.formatted(metodo, monto)));
    }

    private static String generateSecret() {
        byte[] secret = new byte[48];
        new SecureRandom().nextBytes(secret);
        return Base64.getEncoder().encodeToString(secret);
    }

    private record Csrf(Cookie cookie, String headerName, String token) {}
}
