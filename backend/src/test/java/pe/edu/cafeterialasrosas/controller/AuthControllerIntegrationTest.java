package pe.edu.cafeterialasrosas.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cafeterialasrosas.TestCredentials;
import pe.edu.cafeterialasrosas.entity.Rol;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.repository.RolRepository;
import pe.edu.cafeterialasrosas.repository.UsuarioRepository;
import pe.edu.cafeterialasrosas.security.JwtAuthenticationFilter;
import pe.edu.cafeterialasrosas.service.ProductImageStorageService;
import pe.edu.cafeterialasrosas.service.UsuarioService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    private static final String TEST_JWT_SECRET = generateTestSecret();
    private static final String TEST_ADMIN_PASSWORD = TestCredentials.INITIAL_ADMIN_PASSWORD;
    private static final String PASSWORD = generateTestSecret();
    private static final Path TEST_UPLOAD_DIR = Path.of("target", "test-product-images-" + UUID.randomUUID());

    @DynamicPropertySource
    static void configureJwtSecret(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("app.initial-admin.password", () -> TEST_ADMIN_PASSWORD);
        registry.add("app.upload-dir", () -> TEST_UPLOAD_DIR.toString());
    }

    @Test
    void initialAdminIsCreatedWithBcryptAndCanLoginWithoutBeingReset() throws Exception {
        Usuario admin = usuarioRepository.findByCorreoIgnoreCase("admin@cafeteria.com").orElseThrow();
        String originalHash = admin.getPasswordHash();

        org.assertj.core.api.Assertions.assertThat(admin.getNombres()).isEqualTo("Administrador");
        org.assertj.core.api.Assertions.assertThat(admin.getApellidos()).isEqualTo("Sistema");
        org.assertj.core.api.Assertions.assertThat(admin.getRol().getNombre()).isEqualTo("ADMIN");
        org.assertj.core.api.Assertions.assertThat(admin.isActivo()).isTrue();
        org.assertj.core.api.Assertions.assertThat(originalHash).startsWith("$2");
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches(initialAdminPassword, originalHash))
            .isTrue();

        usuarioService.inicializarAdministrador(null);
        usuarioService.inicializarAdministrador("different-password-should-not-be-applied");
        org.assertj.core.api.Assertions.assertThat(
            usuarioRepository.findByCorreoIgnoreCase("admin@cafeteria.com").orElseThrow().getPasswordHash()
        ).isEqualTo(originalHash);

        Csrf csrf = getCsrf();
        MvcResult login = mockMvc.perform(
                loginRequest(csrf, "admin@cafeteria.com", initialAdminPassword)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuario.rol").value("ADMIN"))
            .andReturn();
        org.assertj.core.api.Assertions.assertThat(
            login.getResponse().getCookie(JwtAuthenticationFilter.AUTH_COOKIE_NAME)
        ).isNotNull();
    }

    @Test
    void initialPersonalIsCreatedWithBcryptAndCanLoginWithoutBeingReset() throws Exception {
        Usuario personal = usuarioRepository.findByCorreoIgnoreCase("jhordan@cafeteria.com").orElseThrow();
        String originalHash = personal.getPasswordHash();

        org.assertj.core.api.Assertions.assertThat(personal.getNombres()).isEqualTo("Jhordan");
        org.assertj.core.api.Assertions.assertThat(personal.getApellidos()).isEqualTo("Ramos");
        org.assertj.core.api.Assertions.assertThat(personal.getTelefono()).isEqualTo("982123432");
        org.assertj.core.api.Assertions.assertThat(personal.getRol().getNombre()).isEqualTo("PERSONAL");
        org.assertj.core.api.Assertions.assertThat(personal.isActivo()).isTrue();
        org.assertj.core.api.Assertions.assertThat(originalHash).startsWith("$2");
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches("per123", originalHash)).isTrue();

        usuarioService.inicializarPersonal("another-password");
        org.assertj.core.api.Assertions.assertThat(
            usuarioRepository.findByCorreoIgnoreCase("jhordan@cafeteria.com").orElseThrow().getPasswordHash()
        ).isEqualTo(originalHash);

        Csrf csrf = getCsrf();
        mockMvc.perform(loginRequest(csrf, "jhordan@cafeteria.com", "per123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuario.rol").value("PERSONAL"));
    }

    @Test
    void onlyAdminCanCreatePersonalAccounts() throws Exception {
        String adminEmail = uniqueEmail();
        createUser(adminEmail, "ADMIN", true);
        Csrf loginCsrf = getCsrf();
        Cookie adminCookie = loginAndGetCookie(loginCsrf, adminEmail);
        Csrf adminRequestCsrf = getCsrf();
        String email = uniqueEmail();
        String body = """
            {
              "nombres":"Nuevo",
              "apellidos":"Personal",
              "correo":"%s",
              "telefono":"999888777",
              "password":"personal-password"
            }
            """.formatted(email);

        mockMvc.perform(post("/api/usuarios/personal")
                .cookie(adminCookie, adminRequestCsrf.cookie())
                .header(adminRequestCsrf.headerName(), adminRequestCsrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rol").value("PERSONAL"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());

        Usuario saved = usuarioRepository.findByCorreoIgnoreCase(email).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(saved.getRol().getNombre()).isEqualTo("PERSONAL");
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches(
            "personal-password", saved.getPasswordHash()
        )).isTrue();

        String clienteEmail = uniqueEmail();
        createUser(clienteEmail, "CLIENTE", true);
        Cookie clienteCookie = loginAndGetCookie(getCsrf(), clienteEmail);
        Csrf clienteRequestCsrf = getCsrf();
        mockMvc.perform(post("/api/usuarios/personal")
                .cookie(clienteCookie, clienteRequestCsrf.cookie())
                .header(clienteRequestCsrf.headerName(), clienteRequestCsrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanManageCategoriesProductsAndAvailability() throws Exception {
        String adminEmail = uniqueEmail();
        createUser(adminEmail, "ADMIN", true);
        Cookie adminCookie = loginAndGetCookie(getCsrf(), adminEmail);
        Csrf csrf = getCsrf();

        MvcResult categoryResult = mockMvc.perform(post("/api/categorias")
                .cookie(adminCookie, csrf.cookie())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"Categoria %s","descripcion":"Descripción de prueba"}
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.activo").value(true))
            .andReturn();
        long categoryId = objectMapper.readTree(categoryResult.getResponse().getContentAsString())
            .get("id").asLong();

        MvcResult productResult = mockMvc.perform(post("/api/productos")
                .cookie(adminCookie, csrf.cookie())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoriaId":%d,
                      "nombre":"Producto %s",
                      "descripcion":"Descripción opcional",
                      "precio":12.50,
                      "imagenUrl":null,
                      "disponible":true
                    }
                    """.formatted(categoryId, UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.categoriaId").value(categoryId))
            .andExpect(jsonPath("$.disponible").value(true))
            .andReturn();
        long productId = objectMapper.readTree(productResult.getResponse().getContentAsString())
            .get("id").asLong();

        mockMvc.perform(get("/api/productos/categoria/" + categoryId).cookie(adminCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(productId));

        mockMvc.perform(put("/api/productos/" + productId)
                .cookie(adminCookie, csrf.cookie())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoriaId":%d,
                      "nombre":"Producto actualizado",
                      "descripcion":"Actualizado",
                      "precio":14.25,
                      "imagenUrl":null,
                      "disponible":true
                    }
                    """.formatted(categoryId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Producto actualizado"))
            .andExpect(jsonPath("$.precio").value(14.25));

        mockMvc.perform(patch("/api/productos/" + productId + "/disponibilidad")
                .cookie(adminCookie, csrf.cookie())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"disponible\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.disponible").value(false));

        MvcResult availableResult = mockMvc.perform(get("/api/productos/disponibles").cookie(adminCookie))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode availableProducts = objectMapper.readTree(availableResult.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(availableProducts.findValuesAsText("id"))
            .doesNotContain(Long.toString(productId));
        MvcResult adminProductsResult = mockMvc.perform(get("/api/productos").cookie(adminCookie))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode adminProducts = objectMapper.readTree(adminProductsResult.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(adminProducts.findValuesAsText("id"))
            .contains(Long.toString(productId));

        String customerEmail = uniqueEmail();
        createUser(customerEmail, "CLIENTE", true);
        Cookie customerCookie = loginAndGetCookie(getCsrf(), customerEmail);
        mockMvc.perform(get("/api/productos/" + productId).cookie(customerCookie))
            .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/categorias/" + categoryId + "/estado")
                .cookie(adminCookie, csrf.cookie())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(get("/api/categorias/" + categoryId).cookie(adminCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(false));
        mockMvc.perform(get("/api/categorias/" + categoryId).cookie(customerCookie))
            .andExpect(status().isNotFound());
    }

    @Test
    void adminCanUploadAndRetrieveAProductImage() throws Exception {
        String adminEmail = uniqueEmail();
        createUser(adminEmail, "ADMIN", true);
        Cookie adminCookie = loginAndGetCookie(getCsrf(), adminEmail);
        Csrf csrf = getCsrf();
        String categoryName = "Imagen " + UUID.randomUUID();
        MvcResult category = mockMvc.perform(post("/api/categorias")
                .cookie(adminCookie, csrf.cookie())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"%s","descripcion":null}
                    """.formatted(categoryName)))
            .andExpect(status().isCreated())
            .andReturn();
        long categoryId = objectMapper.readTree(category.getResponse().getContentAsString())
            .get("id").asLong();

        String productJson = """
            {
              "categoriaId":%d,
              "nombre":"Producto con imagen",
              "descripcion":null,
              "precio":8.50,
              "imagenUrl":null,
              "disponible":true
            }
            """.formatted(categoryId);
        byte[] pngSignature = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
        MockMultipartFile productPart = new MockMultipartFile(
            "producto",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            productJson.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile imagePart = new MockMultipartFile(
            "imagen",
            "producto.png",
            MediaType.IMAGE_PNG_VALUE,
            pngSignature
        );

        MvcResult result = mockMvc.perform(multipart("/api/productos")
                .file(productPart)
                .file(imagePart)
                .cookie(adminCookie, csrf.cookie())
                .header(csrf.headerName(), csrf.token()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.imagenUrl").value(
                org.hamcrest.Matchers.startsWith("/uploads/product-images/")
            ))
            .andReturn();
        String imageUrl = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("imagenUrl").asText();

        try {
            mockMvc.perform(get(imageUrl))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                    .contentType(MediaType.IMAGE_PNG));
        } finally {
            productImageStorageService.eliminarSiLocal(imageUrl);
            Files.deleteIfExists(TEST_UPLOAD_DIR);
        }
    }

    @Test
    void clientsAndPersonnelCanReadMenuButCannotManageCategoriesOrProducts() throws Exception {
        for (String role : List.of("CLIENTE", "PERSONAL")) {
            String email = uniqueEmail();
            createUser(email, role, true);
            Cookie userCookie = loginAndGetCookie(getCsrf(), email);
            Csrf csrf = getCsrf();

            mockMvc.perform(get("/api/categorias").cookie(userCookie))
                .andExpect(status().isOk());
            mockMvc.perform(get("/api/productos/disponibles").cookie(userCookie))
                .andExpect(status().isOk());

            mockMvc.perform(post("/api/categorias")
                    .cookie(userCookie, csrf.cookie())
                    .header(csrf.headerName(), csrf.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nombre\":\"No autorizado\",\"descripcion\":null}"))
                .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/productos")
                    .cookie(userCookie, csrf.cookie())
                    .header(csrf.headerName(), csrf.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "categoriaId":1,
                          "nombre":"No autorizado",
                          "descripcion":null,
                          "precio":1.00,
                          "imagenUrl":null,
                          "disponible":true
                        }
                        """))
                .andExpect(status().isForbidden());
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ProductImageStorageService productImageStorageService;

    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Value("${app.initial-admin.password}")
    private String initialAdminPassword;

    @Test
    void registrationHashesPasswordAndAlwaysAssignsCustomerRole() throws Exception {
        String email = uniqueEmail();
        Csrf csrf = getCsrf();

        mockMvc.perform(post("/api/auth/register")
                .cookie(csrf.cookie())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nombres":"Rosa",
                      "apellidos":"Flores",
                      "correo":"%s",
                      "telefono":"999111222",
                      "password":"%s",
                      "rol":"ADMIN"
                    }
                    """.formatted(email, PASSWORD)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rol").value("CLIENTE"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist());

        Usuario saved = usuarioRepository.findByCorreoIgnoreCase(email).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(saved.getRol().getNombre()).isEqualTo("CLIENTE");
        org.assertj.core.api.Assertions.assertThat(saved.getPasswordHash())
            .startsWith("$2")
            .isNotEqualTo(PASSWORD);
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash()))
            .isTrue();
    }

    @Test
    void registrationRejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        Csrf csrf = getCsrf();
        String body = """
            {"nombres":"Rosa","apellidos":"Flores","correo":"%s","password":"%s"}
            """.formatted(email, PASSWORD);

        mockMvc.perform(registerRequest(csrf, body)).andExpect(status().isCreated());
        mockMvc.perform(registerRequest(csrf, body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Ya existe una cuenta con ese correo electrónico."));
    }

    @Test
    void loginSetsHttpOnlyCookieAndMeReturnsPublicUser() throws Exception {
        String email = uniqueEmail();
        createUser(email, "CLIENTE", true);
        Csrf csrf = getCsrf();

        MvcResult login = mockMvc.perform(loginRequest(csrf, email, PASSWORD))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuario.correo").value(email))
            .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"))
            .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist())
            .andExpect(cookie().exists(JwtAuthenticationFilter.AUTH_COOKIE_NAME))
            .andReturn();

        Cookie authCookie = login.getResponse().getCookie(JwtAuthenticationFilter.AUTH_COOKIE_NAME);
        org.assertj.core.api.Assertions.assertThat(authCookie).isNotNull();
        org.assertj.core.api.Assertions.assertThat(authCookie.isHttpOnly()).isTrue();
        org.assertj.core.api.Assertions.assertThat(authCookie.getPath()).isEqualTo("/api");
        org.assertj.core.api.Assertions.assertThat(login.getResponse().getHeader(HttpHeaders.SET_COOKIE))
            .contains("SameSite=Lax");
        org.assertj.core.api.Assertions.assertThat(login.getResponse().getContentAsString())
            .doesNotContain("token")
            .doesNotContain(savedPasswordHash(email));

        mockMvc.perform(get("/api/auth/me").cookie(authCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correo").value(email))
            .andExpect(jsonPath("$.rol").value("CLIENTE"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void invalidCredentialsInactiveUsersAndMissingOrInvalidTokensAreRejected() throws Exception {
        String activeEmail = uniqueEmail();
        createUser(activeEmail, "CLIENTE", true);
        String inactiveEmail = uniqueEmail();
        createUser(inactiveEmail, "CLIENTE", false);
        Csrf csrf = getCsrf();

        mockMvc.perform(loginRequest(csrf, activeEmail, "wrong-password"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Correo o contraseña inválidos."));
        mockMvc.perform(loginRequest(csrf, inactiveEmail, PASSWORD))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Correo o contraseña inválidos."));
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me")
                .cookie(new MockCookie(JwtAuthenticationFilter.AUTH_COOKIE_NAME, "tampered.token.value")))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me")
                .cookie(new MockCookie(JwtAuthenticationFilter.AUTH_COOKIE_NAME, expiredToken(activeEmail))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unsafeAuthenticationRequestsRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"correo":"cliente@example.com","password":"%s"}
                    """.formatted(PASSWORD)))
            .andExpect(status().isForbidden());
    }

    @Test
    void roleRestrictionsAreEnforcedBySpringSecurity() throws Exception {
        String clienteEmail = uniqueEmail();
        String personalEmail = uniqueEmail();
        String adminEmail = uniqueEmail();
        createUser(clienteEmail, "CLIENTE", true);
        createUser(personalEmail, "PERSONAL", true);
        createUser(adminEmail, "ADMIN", true);
        Csrf csrf = getCsrf();
        Cookie clienteCookie = loginAndGetCookie(csrf, clienteEmail);
        Cookie personalCookie = loginAndGetCookie(csrf, personalEmail);
        Cookie adminCookie = loginAndGetCookie(csrf, adminEmail);

        mockMvc.perform(get("/api/admin/roles").cookie(clienteCookie))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/roles").cookie(personalCookie))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/roles").cookie(adminCookie))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/pedidos/operativos").cookie(clienteCookie))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/pedidos/operativos").cookie(personalCookie))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/pedidos/operativos").cookie(adminCookie))
            .andExpect(status().isOk());
    }

    @Test
    void logoutExpiresAuthenticationCookie() throws Exception {
        String email = uniqueEmail();
        createUser(email, "CLIENTE", true);
        Csrf csrf = getCsrf();
        Cookie authCookie = loginAndGetCookie(csrf, email);

        MvcResult logout = mockMvc.perform(post("/api/auth/logout")
                .cookie(authCookie, csrf.cookie())
                .header(csrf.headerName(), csrf.token()))
            .andExpect(status().isNoContent())
            .andReturn();

        String setCookie = String.join(
            ";",
            logout.getResponse().getHeaders(HttpHeaders.SET_COOKIE)
        );
        org.assertj.core.api.Assertions.assertThat(setCookie)
            .contains(JwtAuthenticationFilter.AUTH_COOKIE_NAME)
            .contains("Max-Age=0")
            .contains("HttpOnly");
    }

    @Test
    void csrfEndpointSetsCookieAndAllowsConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(get("/api/auth/csrf").header(HttpHeaders.ORIGIN, "http://localhost:4200"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string("Access-Control-Allow-Origin", "http://localhost:4200"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string("Access-Control-Allow-Credentials", "true"));
    }

    private Csrf getCsrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        org.assertj.core.api.Assertions.assertThat(cookie).isNotNull();
        return new Csrf(cookie, body.get("token").asText(), body.get("headerName").asText());
    }

    private MockHttpServletRequestBuilder registerRequest(Csrf csrf, String body) {
        return post("/api/auth/register")
            .cookie(csrf.cookie())
            .header(csrf.headerName(), csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);
    }

    private MockHttpServletRequestBuilder loginRequest(Csrf csrf, String email, String password) {
        return post("/api/auth/login")
            .cookie(csrf.cookie())
            .header(csrf.headerName(), csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"correo":"%s","password":"%s"}
                """.formatted(email, password));
    }

    private Cookie loginAndGetCookie(Csrf csrf, String email) throws Exception {
        MvcResult result = mockMvc.perform(loginRequest(csrf, email, PASSWORD))
            .andExpect(status().isOk())
            .andReturn();
        return result.getResponse().getCookie(JwtAuthenticationFilter.AUTH_COOKIE_NAME);
    }

    private Usuario createUser(String email, String roleName, boolean active) {
        Rol role = rolRepository.findByNombre(roleName).orElseThrow();
        Usuario usuario = new Usuario();
        usuario.setNombres("Usuario");
        usuario.setApellidos("Prueba");
        usuario.setCorreo(email);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(role);
        usuario.setActivo(active);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private String savedPasswordHash(String email) {
        return usuarioRepository.findByCorreoIgnoreCase(email).orElseThrow().getPasswordHash();
    }

    private String expiredToken(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(subject)
            .issuedAt(Date.from(now.minusSeconds(120)))
            .expiration(Date.from(now.minusSeconds(60)))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    private static String generateTestSecret() {
        byte[] secret = new byte[48];
        new SecureRandom().nextBytes(secret);
        return Base64.getEncoder().encodeToString(secret);
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.test";
    }

    private record Csrf(Cookie cookie, String token, String headerName) {}
}
