package pe.edu.cafeterialasrosas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
        @Value("${app.jwt.secret}") String configuredSecret,
        @Value("${app.jwt.expiration}") long expirationMillis
    ) {
        this.signingKey = createSigningKey(configuredSecret);
        if (expirationMillis <= 0) {
            throw new IllegalStateException("app.jwt.expiration debe ser mayor que cero.");
        }
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(UserPrincipal principal) {
        Instant issuedAt = Instant.now();
        List<String> roles = principal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
        return Jwts.builder()
            .subject(principal.getUsername())
            .claim("uid", principal.getId())
            .claim("roles", roles)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(issuedAt.plusMillis(expirationMillis)))
            .signWith(signingKey)
            .compact();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject() != null
                && claims.getSubject().equalsIgnoreCase(userDetails.getUsername())
                && claims.getExpiration() != null
                && claims.getExpiration().after(new Date())
                && userDetails.isEnabled();
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey createSigningKey(String configuredSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET es obligatorio; configura una clave aleatoria de al menos 32 bytes."
            );
        }
        byte[] keyBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
        if (configuredSecret.startsWith("base64:")) {
            try {
                keyBytes = Decoders.BASE64.decode(configuredSecret.substring("base64:".length()));
            } catch (RuntimeException exception) {
                throw new IllegalStateException("JWT_SECRET con prefijo base64: no es válido.", exception);
            }
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe contener al menos 32 bytes.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
