package com.service.webfluxcourse.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;

@Component
public class TokenManager {

    private  static long MFA8PENDING_TIME=300_000;  // 5mn
    private final SecretKey secretKey;

    private final long accessTokenExpiration;

    private final long refreshTokenExpiration;

    public TokenManager(@Value("${jwt.secret}") String secretKey,
                        @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
                        @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // Generate access token with user details (email + userId)
    public String generateToken(String userId,String email, Set<String> roles) {

        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("roles",roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpiration)))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }


    // validate and parse a token and return claims wrapped in Mono for Reactive processing
    public Optional<Claims> extractClaims(String token) {
        try {
            return Optional.ofNullable(Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }

    }

    public String extractEmail(String token) {
        return extractClaims(token)
                .map(Claims::getSubject)
                .orElse(null);
    }

    public String extractUserId(String token) {

        return extractClaims(token)
                .map(claims -> claims.get("userId", String.class))
                .orElse(null);
    }

    public Date extractExpiration(String token) {
        return extractClaims(token)
                .map(Claims::getExpiration)
                .orElse(null);
    }

    public boolean isExpiredToken(String token) {

        Date expiration = extractExpiration(token);

        if (expiration == null) {
            return true; // token illisible, on considère comme expiré/invalide
        }
        return expiration.before(Date.from(Instant.now()));
    }

    public boolean validateToken(String token) {
        try {
            // Le token est-il expiré ou illisible?
            if (isExpiredToken(token)) {
                return false;  // non valide
            }

            // La signature est-elle bonne ?
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Signature inavlide, token altéré, etc
            return false;
        }

    }


    public String generateRefreshToken(String email) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(email)
                .claim("type", "refresh")
                .expiration(Date.from(now.plusMillis(refreshTokenExpiration)))
                .signWith(secretKey)
                .issuedAt(Date.from(now))
                .compact();
    }

    // Extract authorities from token claims for security context
    public List<GrantedAuthority> getAuthorities(String token) {

        return (List<GrantedAuthority>) extractClaims(token).map(claims -> {
            Object rolesObj = (List<String>) claims.get("roles", List.class);
            if (!(rolesObj instanceof List<?> rolesList)) {
                return List.of();
            }
            List<GrantedAuthority> authorities = new ArrayList<>();
            for (Object object : rolesList) {
                if (object instanceof String role) {
                    authorities.add(new SimpleGrantedAuthority(role));
                }
            }
            return authorities;
        }).orElse(List.of());
    }

    public boolean isRefreshToken(String token) {
        return extractClaims(token)
                .map(claims -> "refresh".equals(claims.get("type", String.class)))
                .orElse(false);
    }


    public String generateTempMFAToken(String email, String userId ) {
        // expiration courte (5 minutes), claim "mfa_pending": true
       return Jwts.builder()
               .subject(email)
               .claim("userId", userId)
               .claim("mfa_pending", true)
               .issuedAt(Date.from(Instant.now()))
               .expiration(Date.from(Instant.now().plusMillis(MFA8PENDING_TIME)))
               .signWith(secretKey)
               .compact();
    }

}
