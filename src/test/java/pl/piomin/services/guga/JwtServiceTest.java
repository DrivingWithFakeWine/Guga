package pl.piomin.services.guga;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import pl.piomin.services.guga.security.JwtService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef-unit-test-secret-0123456789abcdef";

    private final JwtService jwtService = new JwtService(SECRET, 72);

    @Test
    void generateThenParseRoundtrip() {
        String token = jwtService.generate("alice");
        assertEquals("alice", jwtService.parseUsername(token).orElseThrow());
    }

    @Test
    void expirationIsNowPlusTtl() {
        String token = jwtService.generate("alice");
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token).getPayload();
        Instant expiration = claims.getExpiration().toInstant();
        assertTrue(expiration.isAfter(Instant.now().plus(Duration.ofHours(71))));
        assertTrue(expiration.isBefore(Instant.now().plus(Duration.ofHours(73))));
    }

    @Test
    void expiredTokenParsesEmpty() {
        JwtService expiredService = new JwtService(SECRET, -1);
        assertTrue(jwtService.parseUsername(expiredService.generate("alice")).isEmpty());
    }

    @Test
    void tamperedTokenParsesEmpty() {
        String token = jwtService.generate("alice");
        String[] parts = token.split("\\.");
        assertTrue(jwtService.parseUsername(parts[0] + "." + flipChar(parts[1]) + "." + parts[2]).isEmpty());
    }

    @Test
    void tokenFromDifferentKeyParsesEmpty() {
        JwtService other = new JwtService("another-secret-0123456789abcdef-another-secret-0123456789abcdef", 72);
        assertTrue(other.parseUsername(jwtService.generate("alice")).isEmpty());
    }

    @Test
    void shortSecretRejected() {
        assertThrows(WeakKeyException.class, () -> new JwtService("too-short", 72));
    }

    private String flipChar(String segment) {
        return segment.charAt(0) == 'A' ? "B" + segment.substring(1) : "A" + segment.substring(1);
    }
}
