package pe.edu.cafeterialasrosas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pe.edu.cafeterialasrosas.service.UsuarioService;

@Component
public class InitialAdminInitializer implements CommandLineRunner {

    private static final String DEFAULT_INITIAL_PASSWORD = "admin123";
    private static final String INITIAL_PERSONAL_PASSWORD = "per123";

    private final UsuarioService usuarioService;
    private final String initialPassword;

    public InitialAdminInitializer(
        UsuarioService usuarioService,
        @Value("${app.initial-admin.password:admin123}") String initialPassword
    ) {
        this.usuarioService = usuarioService;
        this.initialPassword = initialPassword == null || initialPassword.isBlank()
            ? DEFAULT_INITIAL_PASSWORD
            : initialPassword;
    }

    @Override
    public void run(String... args) {
        usuarioService.inicializarAdministrador(initialPassword);
        usuarioService.inicializarPersonal(INITIAL_PERSONAL_PASSWORD);
    }
}
