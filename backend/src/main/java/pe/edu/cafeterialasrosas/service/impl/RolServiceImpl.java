package pe.edu.cafeterialasrosas.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cafeterialasrosas.dto.response.RolResponse;
import pe.edu.cafeterialasrosas.exception.ResourceNotFoundException;
import pe.edu.cafeterialasrosas.mapper.RolMapper;
import pe.edu.cafeterialasrosas.repository.RolRepository;
import pe.edu.cafeterialasrosas.service.RolService;

@Service
@Transactional(readOnly = true)
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;
    private final RolMapper rolMapper;

    public RolServiceImpl(RolRepository rolRepository, RolMapper rolMapper) {
        this.rolRepository = rolRepository;
        this.rolMapper = rolMapper;
    }

    @Override
    public RolResponse obtenerPorNombre(String nombre) {
        return rolRepository.findByNombre(nombre)
            .map(rolMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el rol solicitado."));
    }

    @Override
    public List<RolResponse> listar() {
        return rolRepository.findAll().stream().map(rolMapper::toResponse).toList();
    }
}
