package pe.edu.cafeterialasrosas.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cafeterialasrosas.dto.request.CategoriaRequest;
import pe.edu.cafeterialasrosas.dto.response.CategoriaResponse;
import pe.edu.cafeterialasrosas.entity.Categoria;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.exception.BusinessRuleException;
import pe.edu.cafeterialasrosas.exception.DuplicateResourceException;
import pe.edu.cafeterialasrosas.exception.ResourceNotFoundException;
import pe.edu.cafeterialasrosas.mapper.CategoriaMapper;
import pe.edu.cafeterialasrosas.repository.CategoriaRepository;
import pe.edu.cafeterialasrosas.repository.UsuarioRepository;
import pe.edu.cafeterialasrosas.service.CategoriaService;

@Service
@Transactional(readOnly = true)
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;
    private final UsuarioRepository usuarioRepository;

    public CategoriaServiceImpl(
        CategoriaRepository categoriaRepository,
        CategoriaMapper categoriaMapper,
        UsuarioRepository usuarioRepository
    ) {
        this.categoriaRepository = categoriaRepository;
        this.categoriaMapper = categoriaMapper;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<CategoriaResponse> listarActivas() {
        return categoriaRepository.findByActivoTrueOrderByNombreAsc()
            .stream()
            .map(categoriaMapper::toResponse)
            .toList();
    }

    @Override
    public List<CategoriaResponse> listarTodas() {
        return categoriaRepository.findAll()
            .stream()
            .sorted((first, second) -> first.getNombre().compareToIgnoreCase(second.getNombre()))
            .map(categoriaMapper::toResponse)
            .toList();
    }

    @Override
    public CategoriaResponse obtenerActivaPorId(Long categoriaId) {
        Categoria categoria = buscar(categoriaId);
        if (!categoria.isActivo()) {
            throw new ResourceNotFoundException("No se encontró la categoría solicitada.");
        }
        return categoriaMapper.toResponse(categoria);
    }

    @Override
    public CategoriaResponse obtenerPorId(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
            .map(categoriaMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró la categoría solicitada."));
    }

    @Override
    @Transactional
    public CategoriaResponse crear(CategoriaRequest request, Usuario administradorSolicitante) {
        Usuario administrador = AutorizacionOperativa.cargarUsuarioActivo(
            administradorSolicitante,
            usuarioRepository
        );
        AutorizacionOperativa.requiereAdmin(administrador);
        validarRequest(request);
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new DuplicateResourceException("Ya existe una categoría con ese nombre.");
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(request.descripcion());
        categoria.setActivo(true);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaResponse actualizar(
        Long categoriaId,
        CategoriaRequest request,
        Usuario administradorSolicitante
    ) {
        Usuario administrador = AutorizacionOperativa.cargarUsuarioActivo(
            administradorSolicitante,
            usuarioRepository
        );
        AutorizacionOperativa.requiereAdmin(administrador);
        validarRequest(request);
        Categoria categoria = buscar(categoriaId);
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, categoriaId)) {
            throw new DuplicateResourceException("Ya existe una categoría con ese nombre.");
        }
        categoriaMapper.update(categoria, request);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaResponse cambiarEstado(
        Long categoriaId,
        boolean activa,
        Usuario administradorSolicitante
    ) {
        Usuario administrador = AutorizacionOperativa.cargarUsuarioActivo(
            administradorSolicitante,
            usuarioRepository
        );
        AutorizacionOperativa.requiereAdmin(administrador);
        Categoria categoria = buscar(categoriaId);
        categoria.setActivo(activa);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    private Categoria buscar(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró la categoría solicitada."));
    }

    private void validarRequest(CategoriaRequest request) {
        if (request == null || request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessRuleException("El nombre de la categoría es obligatorio.");
        }
    }
}
