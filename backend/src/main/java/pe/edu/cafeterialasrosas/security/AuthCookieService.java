package pe.edu.cafeterialasrosas.security;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {

    private final boolean secure;
    private final String sameSite;
    private final long expirationMillis;

    public AuthCookieService(
        @Value("${app.jwt.cookie-secure:false}") boolean secure,
        @Value("${app.jwt.cookie-same-site:Lax}") String sameSite,
        @Value("${app.jwt.expiration}") long expirationMillis
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.expirationMillis = expirationMillis;
    }

    public ResponseCookie create(String token) {
        return ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE_NAME, token)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/api")
            .maxAge(Duration.ofMillis(expirationMillis))
            .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/api")
            .maxAge(Duration.ZERO)
            .build();
    }
}
