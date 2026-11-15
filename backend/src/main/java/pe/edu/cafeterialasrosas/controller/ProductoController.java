package pe.edu.cafeterialasrosas.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.cafeterialasrosas.dto.request.DisponibilidadProductoRequest;
import pe.edu.cafeterialasrosas.dto.request.ProductoRequest;
import pe.edu.cafeterialasrosas.dto.response.ProductoResponse;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.security.UserPrincipal;
import pe.edu.cafeterialasrosas.service.ProductImageStorageService;
import pe.edu.cafeterialasrosas.service.ProductoService;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final ProductImageStorageService productImageStorageService;

    public ProductoController(
        ProductoService productoService,
        ProductImageStorageService productImageStorageService
    ) {
        this.productoService = productoService;
        this.productImageStorageService = productImageStorageService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'PERSONAL', 'ADMIN')")
    public List<ProductoResponse> listar(@AuthenticationPrincipal UserPrincipal principal) {
        return esAdmin(principal) ? productoService.listarTodos() : productoService.listarDisponibles();
    }

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PERSONAL', 'ADMIN')")
    public List<ProductoResponse> listarDisponibles() {
        return productoService.listarDisponibles();
    }

    @GetMapping("/categoria/{categoriaId}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PERSONAL', 'ADMIN')")
    public List<ProductoResponse> listarPorCategoria(
        @PathVariable Long categoriaId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return esAdmin(principal)
            ? productoService.listarTodosPorCategoria(categoriaId)
            : productoService.listarPorCategoria(categoriaId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PERSONAL', 'ADMIN')")
    public ProductoResponse obtener(
        @PathVariable Long id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return esAdmin(principal)
            ? productoService.obtenerPorId(id)
            : productoService.obtenerDisponiblePorId(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> crear(
        @Valid @RequestBody ProductoRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(productoService.crear(request, actor(principal)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> crearConImagen(
        @Valid @RequestPart("producto") ProductoRequest request,
        @RequestPart("imagen") MultipartFile imagen,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        String imageUrl = productImageStorageService.guardar(imagen);
        try {
            ProductoResponse producto = productoService.crear(conImagenUrl(request, imageUrl), actor(principal));
            return ResponseEntity.status(HttpStatus.CREATED).body(producto);
        } catch (RuntimeException exception) {
            productImageStorageService.eliminarSiLocal(imageUrl);
            throw exception;
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ProductoRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return productoService.actualizar(id, request, actor(principal));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse actualizarConImagen(
        @PathVariable Long id,
        @Valid @RequestPart("producto") ProductoRequest request,
        @RequestPart("imagen") MultipartFile imagen,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        String imagenAnterior = productoService.obtenerPorId(id).imagenUrl();
        String nuevaImagen = productImageStorageService.guardar(imagen);
        ProductoResponse producto;
        try {
            producto = productoService.actualizar(id, conImagenUrl(request, nuevaImagen), actor(principal));
        } catch (RuntimeException exception) {
            productImageStorageService.eliminarSiLocal(nuevaImagen);
            throw exception;
        }
        productImageStorageService.eliminarSiLocal(imagenAnterior);
        return producto;
    }

    @PatchMapping("/{id}/disponibilidad")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse cambiarDisponibilidad(
        @PathVariable Long id,
        @Valid @RequestBody DisponibilidadProductoRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return productoService.cambiarDisponibilidad(id, request.disponible(), actor(principal));
    }

    private Usuario actor(UserPrincipal principal) {
        Usuario usuario = new Usuario();
        usuario.setId(principal.getId());
        return usuario;
    }

    private ProductoRequest conImagenUrl(ProductoRequest request, String imageUrl) {
        return new ProductoRequest(
            request.categoriaId(),
            request.nombre(),
            request.descripcion(),
            request.precio(),
            imageUrl,
            request.disponible()
        );
    }

    private boolean esAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
    }
}
