package pe.edu.cafeterialasrosas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Email @Size(max = 254) String correo,
    @NotBlank @Size(max = 72) String password
) {

    @Override
    public String toString() {
        return "LoginRequest[correo=" + correo + ", password=[REDACTED]]";
    }
}
