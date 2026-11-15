package pe.edu.cafeterialasrosas.service;

import java.util.List;
import pe.edu.cafeterialasrosas.dto.response.RolResponse;

public interface RolService {

    RolResponse obtenerPorNombre(String nombre);

    List<RolResponse> listar();
}
