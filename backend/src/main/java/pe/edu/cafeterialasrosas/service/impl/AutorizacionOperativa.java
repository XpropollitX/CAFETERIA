package pe.edu.cafeterialasrosas.service.impl;

import pe.edu.cafeterialasrosas.entity.Usuario;
import pe.edu.cafeterialasrosas.exception.OperationNotAllowedException;
import pe.edu.cafeterialasrosas.exception.ResourceNotFoundException;
import pe.edu.cafeterialasrosas.repository.UsuarioRepository;

final class AutorizacionOperativa {

    private AutorizacionOperativa() {}

    static void requierePersonalOAdmin(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) {
            throw new OperationNotAllowedException("Se requiere una cuenta autorizada para esta operación.");
        }
        String rol = usuario.getRol().getNombre();
        if (!"PERSONAL".equals(rol) && !"ADMIN".equals(rol)) {
            throw new OperationNotAllowedException("Esta operación requiere el rol PERSONAL o ADMIN.");
        }
    }

    static boolean esPersonalOAdmin(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) {
            return false;
        }
        String rol = usuario.getRol().getNombre();
        return "PERSONAL".equals(rol) || "ADMIN".equals(rol);
    }

    static void requiereAdmin(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null || !"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new OperationNotAllowedException("Esta operación requiere el rol ADMIN.");
        }
    }

    static Usuario cargarUsuarioActivo(Usuario solicitante, UsuarioRepository usuarioRepository) {
        if (solicitante == null || solicitante.getId() == null) {
            throw new OperationNotAllowedException("Se requiere una cuenta activa para esta operación.");
        }
        Usuario actor = usuarioRepository.findById(solicitante.getId())
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario solicitado."));
        if (!actor.isActivo()) {
            throw new OperationNotAllowedException("La cuenta está inactiva.");
        }
        return actor;
    }
}
