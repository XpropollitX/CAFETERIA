package pe.edu.cafeterialasrosas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarUsuarioRequest(
    @NotBlank @Size(max = 100) String nombres,
    @NotBlank @Size(max = 100) String apellidos,
    @NotBlank @Email @Size(max = 254) String correo,
    @Size(max = 20) String telefono,
    @NotBlank @Size(min = 8, max = 72) String password
) {

    @Override
    public String toString() {
        return "RegistrarUsuarioRequest[nombres=" + nombres
            + ", apellidos=" + apellidos
            + ", correo=" + correo
            + ", telefono=" + telefono
            + ", password=[REDACTED]]";
    }
}
