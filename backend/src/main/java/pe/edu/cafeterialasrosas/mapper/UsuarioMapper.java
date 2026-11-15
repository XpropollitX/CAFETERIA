package pe.edu.cafeterialasrosas.mapper;

import org.springframework.stereotype.Component;
import pe.edu.cafeterialasrosas.dto.response.UsuarioResponse;
import pe.edu.cafeterialasrosas.entity.Usuario;

@Component
public class UsuarioMapper {

    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
            usuario.getId(),
            usuario.getNombres(),
            usuario.getApellidos(),
            usuario.getCorreo(),
            usuario.getTelefono(),
            usuario.getRol().getNombre(),
            usuario.isActivo(),
            usuario.getFechaCreacion()
        );
    }
}
