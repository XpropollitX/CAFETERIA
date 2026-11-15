package pe.edu.cafeterialasrosas.mapper;

import org.springframework.stereotype.Component;
import pe.edu.cafeterialasrosas.dto.request.CategoriaRequest;
import pe.edu.cafeterialasrosas.dto.response.CategoriaResponse;
import pe.edu.cafeterialasrosas.entity.Categoria;

@Component
public class CategoriaMapper {

    public CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(
            categoria.getId(),
            categoria.getNombre(),
            categoria.getDescripcion(),
            categoria.isActivo()
        );
    }

    public void update(Categoria categoria, CategoriaRequest request) {
        categoria.setNombre(request.nombre().trim());
        categoria.setDescripcion(request.descripcion());
    }
}
