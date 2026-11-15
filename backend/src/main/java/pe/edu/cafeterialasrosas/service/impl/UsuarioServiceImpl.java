package pe.edu.cafeterialasrosas.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.cafeterialasrosas.dto.request.RegistrarUsuarioRequest;
import pe.edu.cafeterialasrosas.dto.response.UsuarioResponse;
import pe.edu.cafeterialasrosas.exception.BusinessRuleException;
import pe.edu.cafeterialasrosas.exception.DuplicateResourceException;
import pe.edu.cafeterialasrosas.exception.ResourceNotFoundException;
import pe.edu.cafeterialasrosas.entity.Rol;
import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.mapper.UsuarioMapper;
import pe.edu.cafeterialasrosas.repository.RolRepository;
import pe.edu.cafeterialasrosas.repository.UsuarioRepository;
import pe.edu.cafeterialasrosas.service.UsuarioService;

@Service
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(
        UsuarioRepository usuarioRepository,
        UsuarioMapper usuarioMapper,
        RolRepository rolRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UsuarioResponse obtenerPorId(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
            .map(usuarioMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario solicitado."));
    }

    @Override
    public UsuarioResponse obtenerPorCorreo(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo.trim())
            .map(usuarioMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario solicitado."));
    }

    @Override
    public void validarCorreoDisponible(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new BusinessRuleException("El correo electrónico es obligatorio.");
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(correo.trim())) {
            throw new DuplicateResourceException("Ya existe una cuenta con ese correo electrónico.");
        }
    }

    @Override
    @Transactional
    public UsuarioResponse registrarCliente(RegistrarUsuarioRequest request) {
        return registrarUsuarioConRol(request, "CLIENTE");
    }

    @Override
    @Transactional
    public UsuarioResponse registrarPersonal(RegistrarUsuarioRequest request) {
        return registrarUsuarioConRol(request, "PERSONAL");
    }

    private UsuarioResponse registrarUsuarioConRol(RegistrarUsuarioRequest request, String nombreRol) {
        if (request == null || request.nombres() == null || request.nombres().isBlank()
            || request.apellidos() == null || request.apellidos().isBlank()
            || request.correo() == null || request.correo().isBlank()
            || request.password() == null || request.password().isBlank()) {
            throw new BusinessRuleException("Los campos obligatorios del registro son inválidos.");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessRuleException("La contraseña no puede superar 72 bytes UTF-8.");
        }

        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        validarCorreoDisponible(correo);
        Rol rol = rolRepository.findByNombre(nombreRol)
            .orElseThrow(() -> new ResourceNotFoundException("No está configurado el rol solicitado."));

        Usuario usuario = new Usuario();
        usuario.setNombres(request.nombres().trim());
        usuario.setApellidos(request.apellidos().trim());
        usuario.setCorreo(correo);
        usuario.setTelefono(request.telefono() == null || request.telefono().isBlank()
            ? null
            : request.telefono().trim());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(rol);
        usuario.setActivo(true);

        return usuarioMapper.toResponse(usuarioRepository.saveAndFlush(usuario));
    }

    @Override
    @Transactional
    public void inicializarAdministrador(String passwordInicial) {
        String correo = "admin@cafeteria.com";
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            return;
        }
        if (passwordInicial == null || passwordInicial.isBlank()) {
            throw new IllegalStateException(
                "Configura ADMIN_INITIAL_PASSWORD para crear la cuenta inicial de administrador."
            );
        }
        if (passwordInicial.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                "ADMIN_INITIAL_PASSWORD no puede superar 72 bytes UTF-8."
            );
        }

        Rol rolAdmin = rolRepository.findByNombre("ADMIN")
            .orElseThrow(() -> new ResourceNotFoundException("No está configurado el rol ADMIN."));

        Usuario administrador = new Usuario();
        administrador.setNombres("Administrador");
        administrador.setApellidos("Sistema");
        administrador.setCorreo(correo);
        administrador.setPasswordHash(passwordEncoder.encode(passwordInicial));
        administrador.setRol(rolAdmin);
        administrador.setActivo(true);
        usuarioRepository.saveAndFlush(administrador);
    }

    @Override
    @Transactional
    public void inicializarPersonal(String passwordInicial) {
        String correo = "jhordan@cafeteria.com";
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            return;
        }
        if (passwordInicial == null || passwordInicial.isBlank()) {
            throw new IllegalStateException("La contraseña inicial de PERSONAL es obligatoria.");
        }
        if (passwordInicial.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("La contraseña inicial de PERSONAL no puede superar 72 bytes UTF-8.");
        }

        Rol rolPersonal = rolRepository.findByNombre("PERSONAL")
            .orElseThrow(() -> new ResourceNotFoundException("No está configurado el rol PERSONAL."));

        Usuario personal = new Usuario();
        personal.setNombres("Jhordan");
        personal.setApellidos("Ramos");
        personal.setCorreo(correo);
        personal.setTelefono("982123432");
        personal.setPasswordHash(passwordEncoder.encode(passwordInicial));
        personal.setRol(rolPersonal);
        personal.setActivo(true);
        usuarioRepository.saveAndFlush(personal);
    }
}
