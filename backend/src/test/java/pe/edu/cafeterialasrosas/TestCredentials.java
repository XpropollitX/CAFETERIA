package pe.edu.cafeterialasrosas;

import java.security.SecureRandom;
import java.util.Base64;

public final class TestCredentials {

    public static final String INITIAL_ADMIN_PASSWORD =
        System.getenv().getOrDefault("ADMIN_INITIAL_PASSWORD", randomValue());

    private TestCredentials() {}

    private static String randomValue() {
        byte[] value = new byte[24];
        new SecureRandom().nextBytes(value);
        return Base64.getEncoder().encodeToString(value);
    }
}
