package pe.edu.cafeterialasrosas.service;

import java.util.List;
import pe.edu.cafeterialasrosas.dto.request.CategoriaRequest;
import pe.edu.cafeterialasrosas.dto.response.CategoriaResponse;
import pe.edu.cafeterialasrosas.entity.Usuario;

public interface CategoriaService {

    List<CategoriaResponse> listarActivas();

    List<CategoriaResponse> listarTodas();

    CategoriaResponse obtenerActivaPorId(Long categoriaId);

    CategoriaResponse obtenerPorId(Long categoriaId);

    CategoriaResponse crear(CategoriaRequest request, Usuario administrador);

    CategoriaResponse actualizar(Long categoriaId, CategoriaRequest request, Usuario administrador);

    CategoriaResponse cambiarEstado(Long categoriaId, boolean activa, Usuario administrador);
}
