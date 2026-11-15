package pe.edu.cafeterialasrosas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import pe.edu.cafeterialasrosas.dto.response.ApiErrorResponse;

@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        org.springframework.security.core.AuthenticationException exception
    ) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "Autenticación requerida o credenciales inválidas.");
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException exception
    ) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta operación.");
    }

    public void writeUnauthorized(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "La sesión no es válida o ha expirado.");
    }

    private void write(HttpServletResponse response, HttpStatus status, String message)
        throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
            response.getOutputStream(),
            new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                Map.of()
            )
        );
    }
}
