package pe.edu.cafeterialasrosas.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.validation.ConstraintViolationException;
import pe.edu.cafeterialasrosas.dto.response.ApiErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingEndpoint(NoResourceFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "No se encontró el recurso solicitado.", Map.of());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    public ResponseEntity<ApiErrorResponse> handleOperationNotAllowed(
        OperationNotAllowedException exception
    ) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailure(
        AuthenticationException exception
    ) {
        return error(
            HttpStatus.UNAUTHORIZED,
            "Correo o contraseña inválidos.",
            Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidArguments(
        MethodArgumentNotValidException exception
    ) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            validationErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Hay campos con datos inválidos.", validationErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
        ConstraintViolationException exception
    ) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
            validationErrors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        return error(HttpStatus.BAD_REQUEST, "Hay parámetros con datos inválidos.", validationErrors);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "La solicitud contiene datos con formato inválido.", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
        DataIntegrityViolationException exception
    ) {
        return error(
            HttpStatus.CONFLICT,
            "La operación entra en conflicto con datos existentes o restricciones del sistema.",
            Map.of()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(
        MaxUploadSizeExceededException exception
    ) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen no puede superar los 5 MB.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        logger.error("Error inesperado al procesar la solicitud", exception);
        return error(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ocurrió un error interno. Inténtalo nuevamente más tarde.",
            Map.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> error(
        HttpStatus status,
        String message,
        Map<String, String> validationErrors
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            validationErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
