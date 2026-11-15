package pe.edu.cafeterialasrosas.service;

import pe.edu.cafeterialasrosas.dto.request.RegistrarUsuarioRequest;
import pe.edu.cafeterialasrosas.dto.response.UsuarioResponse;

public interface UsuarioService {

    UsuarioResponse obtenerPorId(Long usuarioId);

    UsuarioResponse obtenerPorCorreo(String correo);

    void validarCorreoDisponible(String correo);

    UsuarioResponse registrarCliente(RegistrarUsuarioRequest request);

    UsuarioResponse registrarPersonal(RegistrarUsuarioRequest request);

    void inicializarAdministrador(String passwordInicial);

    void inicializarPersonal(String passwordInicial);
}
