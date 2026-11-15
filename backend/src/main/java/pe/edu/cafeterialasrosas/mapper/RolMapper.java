package pe.edu.cafeterialasrosas.mapper;

import org.springframework.stereotype.Component;
import pe.edu.cafeterialasrosas.dto.response.RolResponse;
import pe.edu.cafeterialasrosas.entity.Rol;

@Component
public class RolMapper {

    public RolResponse toResponse(Rol rol) {
        return new RolResponse(rol.getId(), rol.getNombre(), rol.getDescripcion());
    }
}
