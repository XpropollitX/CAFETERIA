package pe.edu.cafeterialasrosas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.security.SecureRandom;
import java.util.Base64;

@SpringBootTest
class CafeteriaLasRosasBackendApplicationTests {

	@DynamicPropertySource
	static void configureTestJwtSecret(DynamicPropertyRegistry registry) {
		byte[] secret = new byte[48];
		new SecureRandom().nextBytes(secret);
		registry.add("app.jwt.secret", () -> Base64.getEncoder().encodeToString(secret));
		registry.add("app.initial-admin.password", () -> TestCredentials.INITIAL_ADMIN_PASSWORD);
	}

	@Test
	void contextLoads() {
	}

}
