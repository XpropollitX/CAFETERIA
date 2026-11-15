package pe.edu.cafeterialasrosas.mapper;

import org.springframework.stereotype.Component;
import pe.edu.cafeterialasrosas.dto.request.ProductoRequest;
import pe.edu.cafeterialasrosas.dto.response.ProductoResponse;
import pe.edu.cafeterialasrosas.entity.Producto;

@Component
public class ProductoMapper {

    public ProductoResponse toResponse(Producto producto) {
        return new ProductoResponse(
            producto.getId(),
            producto.getCategoria().getId(),
            producto.getCategoria().getNombre(),
            producto.getNombre(),
            producto.getDescripcion(),
            producto.getPrecio(),
            producto.getImagenUrl(),
            producto.isDisponible(),
            producto.getFechaCreacion()
        );
    }

    public void update(Producto producto, ProductoRequest request) {
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setImagenUrl(request.imagenUrl());
        producto.setDisponible(request.disponible());
    }
}
