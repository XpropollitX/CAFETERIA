package pe.edu.cafeterialasrosas.service;

import java.util.List;
import pe.edu.cafeterialasrosas.dto.request.ProductoRequest;
import pe.edu.cafeterialasrosas.dto.response.ProductoResponse;
import pe.edu.cafeterialasrosas.entity.Usuario;

public interface ProductoService {

    List<ProductoResponse> listarDisponibles();

    List<ProductoResponse> listarTodos();

    List<ProductoResponse> listarPorCategoria(Long categoriaId);

    List<ProductoResponse> listarTodosPorCategoria(Long categoriaId);

    ProductoResponse obtenerDisponiblePorId(Long productoId);

    ProductoResponse obtenerPorId(Long productoId);

    ProductoResponse crear(ProductoRequest request, Usuario administrador);

    ProductoResponse actualizar(Long productoId, ProductoRequest request, Usuario administrador);

    ProductoResponse cambiarDisponibilidad(Long productoId, boolean disponible, Usuario administrador);
}
