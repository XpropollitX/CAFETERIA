package pe.edu.cafeterialasrosas.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cafeterialasrosas.dto.request.ProductoRequest;
import pe.edu.cafeterialasrosas.dto.response.ProductoResponse;
import pe.edu.cafeterialasrosas.entity.Categoria;
import pe.edu.cafeterialasrosas.entity.Producto;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.exception.BusinessRuleException;
import pe.edu.cafeterialasrosas.exception.ResourceNotFoundException;
import pe.edu.cafeterialasrosas.mapper.ProductoMapper;
import pe.edu.cafeterialasrosas.repository.CategoriaRepository;
import pe.edu.cafeterialasrosas.repository.ProductoRepository;
import pe.edu.cafeterialasrosas.repository.UsuarioRepository;
import pe.edu.cafeterialasrosas.service.ProductoService;

@Service
@Transactional(readOnly = true)
public class ProductoServiceImpl implements ProductoService {

    private static final BigDecimal PRECIO_MINIMO = new BigDecimal("0.01");

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoMapper productoMapper;
    private final UsuarioRepository usuarioRepository;

    public ProductoServiceImpl(
        ProductoRepository productoRepository,
        CategoriaRepository categoriaRepository,
        ProductoMapper productoMapper,
        UsuarioRepository usuarioRepository
    ) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoMapper = productoMapper;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<ProductoResponse> listarDisponibles() {
        return productoRepository.findByDisponibleTrueAndCategoria_ActivoTrueOrderByNombreAsc()
            .stream()
            .map(productoMapper::toResponse)
            .toList();
    }

    @Override
    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAll()
            .stream()
            .sorted((first, second) -> first.getNombre().compareToIgnoreCase(second.getNombre()))
            .map(productoMapper::toResponse)
            .toList();
    }

    @Override
    public List<ProductoResponse> listarPorCategoria(Long categoriaId) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró la categoría solicitada."));
        if (!categoria.isActivo()) {
            throw new ResourceNotFoundException("No se encontró la categoría solicitada.");
        }
        return productoRepository.findByCategoria_IdAndDisponibleTrueOrderByNombreAsc(categoriaId)
            .stream().map(productoMapper::toResponse).toList();
    }

    @Override
    public List<ProductoResponse> listarTodosPorCategoria(Long categoriaId) {
        if (!categoriaRepository.existsById(categoriaId)) {
            throw new ResourceNotFoundException("No se encontró la categoría solicitada.");
        }
        return productoRepository.findByCategoria_IdOrderByNombreAsc(categoriaId)
            .stream()
            .map(productoMapper::toResponse)
            .toList();
    }

    @Override
    public ProductoResponse obtenerDisponiblePorId(Long productoId) {
        Producto producto = buscarProducto(productoId);
        if (!producto.isDisponible() || !producto.getCategoria().isActivo()) {
            throw new ResourceNotFoundException("No se encontró el producto solicitado.");
        }
        return productoMapper.toResponse(producto);
    }

    @Override
    public ProductoResponse obtenerPorId(Long productoId) {
        return productoRepository.findById(productoId)
            .map(productoMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el producto solicitado."));
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request, Usuario administradorSolicitante) {
        Usuario administrador = AutorizacionOperativa.cargarUsuarioActivo(
            administradorSolicitante,
            usuarioRepository
        );
        AutorizacionOperativa.requiereAdmin(administrador);
        validarRequest(request);
        validarPrecio(request.precio());
        Categoria categoria = buscarCategoriaActiva(request.categoriaId());
        Producto producto = new Producto();
        producto.setCategoria(categoria);
        productoMapper.update(producto, request);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(
        Long productoId,
        ProductoRequest request,
        Usuario administradorSolicitante
    ) {
        Usuario administrador = AutorizacionOperativa.cargarUsuarioActivo(
            administradorSolicitante,
            usuarioRepository
        );
        AutorizacionOperativa.requiereAdmin(administrador);
        validarRequest(request);
        validarPrecio(request.precio());
        Producto producto = buscarProducto(productoId);
        producto.setCategoria(buscarCategoriaActiva(request.categoriaId()));
        productoMapper.update(producto, request);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse cambiarDisponibilidad(
        Long productoId,
        boolean disponible,
        Usuario administradorSolicitante
    ) {
        Usuario administrador = AutorizacionOperativa.cargarUsuarioActivo(
            administradorSolicitante,
            usuarioRepository
        );
        AutorizacionOperativa.requiereAdmin(administrador);
        Producto producto = buscarProducto(productoId);
        if (disponible && !producto.getCategoria().isActivo()) {
            throw new BusinessRuleException("No se puede habilitar un producto de una categoría inactiva.");
        }
        producto.setDisponible(disponible);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    private void validarPrecio(BigDecimal precio) {
        if (precio == null || precio.compareTo(PRECIO_MINIMO) < 0) {
            throw new BusinessRuleException("El precio del producto debe ser mayor que cero.");
        }
    }

    private void validarRequest(ProductoRequest request) {
        if (request == null || request.categoriaId() == null || request.nombre() == null
            || request.nombre().isBlank() || request.disponible() == null) {
            throw new BusinessRuleException("Los datos obligatorios del producto son inválidos.");
        }
    }

    private Categoria buscarCategoriaActiva(Long categoriaId) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró la categoría solicitada."));
        if (!categoria.isActivo()) {
            throw new BusinessRuleException("No se puede asociar un producto a una categoría inactiva.");
        }
        return categoria;
    }

    private Producto buscarProducto(Long productoId) {
        return productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el producto solicitado."));
    }
}
