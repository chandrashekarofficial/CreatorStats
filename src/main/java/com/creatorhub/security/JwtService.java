package com.creatorhub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private static final long OAUTH_STATE_EXPIRATION_MS = 10 * 60 * 1000L;

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {
        Date now = new Date();
        return Jwts.builder().subject(email).issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key).compact();
    }

    public String generateOAuthState(String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("purpose", "youtube-connect")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + OAUTH_STATE_EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public String validateOAuthState(String state) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(state).getPayload();
            if (!"youtube-connect".equals(claims.get("purpose", String.class))) {
                throw new IllegalArgumentException("Invalid OAuth state.");
            }
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid or expired YouTube connection request.");
        }
    }

    public boolean isValid(String token, String email) {
        try { return extractEmail(token).equalsIgnoreCase(email); }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }
}
