package com.offerhub.identity.security;

import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey key;
    private final long accessTokenExpirySeconds;
    private final long refreshTokenExpirySeconds;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry-seconds}") long accessTokenExpirySeconds,
            @Value("${jwt.refresh-token-expiry-seconds}") long refreshTokenExpirySeconds
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpirySeconds = accessTokenExpirySeconds;
        this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
    }

    public String generateAccessToken(String subjectId, String role) {
        return buildToken(subjectId, role, accessTokenExpirySeconds, "access", null);
    }

    /**
     * tokenId becomes the JWT's "jti" claim. The caller mints it (a fresh UUID) and
     * persists a matching RefreshToken row under the same id before handing the token to
     * the client - that row is what makes rotation and reuse detection possible, since a
     * signature alone cannot tell a legitimate refresh from a replayed one.
     */
    public String generateRefreshToken(String subjectId, String role, String tokenId) {
        return buildToken(subjectId, role, refreshTokenExpirySeconds, "refresh", tokenId);
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpirySeconds;
    }

    public long getRefreshTokenExpirySeconds() {
        return refreshTokenExpirySeconds;
    }

    public Jws<Claims> parseToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    private String buildToken(String subjectId, String role, long expirySeconds, String tokenType, String tokenId) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(subjectId)
                .claim("role", role)
                .claim("type", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(key);
        if (tokenId != null) {
            builder.id(tokenId);
        }
        return builder.compact();
    }
}