package com.voltx.security;

import com.voltx.config.VoltXProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and verifies HS256-signed JWTs.
 * The signing key comes from {@code VOLTX_JWT_SECRET}; if that is not set (local development),
 * a random key is generated at start-up so no secret is ever committed to the repository.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final long expirationMinutes;
    private final Clock clock;

    public JwtService(VoltXProperties properties, Clock clock) {
        this.clock = clock;
        this.expirationMinutes = properties.jwt().expirationMinutes();
        String secret = properties.jwt().secret();
        if (secret == null || secret.isBlank()) {
            log.warn("VOLTX_JWT_SECRET is not set - using a random signing key (tokens reset on restart)");
            this.signingKey = Jwts.SIG.HS256.key().build();
        } else {
            byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < MIN_SECRET_BYTES) {
                throw new IllegalStateException("VOLTX_JWT_SECRET must be at least 32 characters");
            }
            this.signingKey = Keys.hmacShaKeyFor(bytes);
        }
    }

    public IssuedToken issue(VoltXUserDetails user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        String token = Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /** Returns the e-mail (subject) when the token is valid and not expired. */
    public Optional<String> validateAndGetSubject(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected JWT: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
