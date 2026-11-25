package com.service.webfluxcourse.security;

import com.service.webfluxcourse.domain.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class TokenManagerTest {

    private TokenManager tokenManager;
    private User user;
    long accessTokenExpiration = 900_000; // 15mn
    long refreshTokenExpiration = 604_800_000;  // 7 jours

    @BeforeEach
    void setUp() {
        byte[] bytesSecret = new byte[32];
        new SecureRandom().nextBytes(bytesSecret);
        Set<String> roles = new HashSet<>(Arrays.asList("ROLE_USER", "ROLE_ADMIN", "ROLE_STUDENT"));
        String secretKeyString = Base64.getEncoder().encodeToString(bytesSecret);
        tokenManager = new TokenManager(secretKeyString, accessTokenExpiration, refreshTokenExpiration);
        user = new User(UUID.randomUUID(), "test@example.com", "rttrrytutTYYUUI667",
                "Dede", roles, true,
                false, true, Instant.now(), Instant.now());
    }

    @Test
    void generate_valid_Token() {
        String jwtGenerated = tokenManager.generateToken(user.email(), user.userId().toString(), user.roles());
        assertThat(jwtGenerated).isNotEmpty();
    }

    @Test
    void extractClaims_test() {
        String jwtGenerated = tokenManager.generateToken(user.email(), user.userId().toString(), user.roles());
        Optional<Claims> claims = tokenManager.extractClaims(jwtGenerated);
        assertThat(claims.isPresent()).isTrue();
    }

    @Test
    void extract_Email_from_claims() {
        String jwtGenerated = tokenManager.generateToken(user.email(), user.userId().toString(), user.roles());
        Optional<Claims> claims = tokenManager.extractClaims(jwtGenerated);
        claims.ifPresent(claim -> {
            String email = claim.getSubject();
            assertThat(email).isEqualTo(user.email());
        });

    }

    @Test
    void extractUserId() {
        String jwtGenerated = tokenManager.generateToken(user.email(), user.userId().toString(), user.roles());
        Optional<Claims> claims = tokenManager.extractClaims(jwtGenerated);
        claims.ifPresent(claim -> {
            String userId = claim.getSubject();
            assertThat(userId).isEqualTo(user.email());
        });
    }

    @Test
    void extract_Expiration_is_not_expired() {
        String jwtGenerated = tokenManager.generateToken(user.email(), user.userId().toString(), user.roles());
        Optional<Claims> claims = tokenManager.extractClaims(jwtGenerated);
        claims.ifPresent(claim -> {
            long exp = claim.getExpiration().getTime();
            assertThat(exp).isGreaterThanOrEqualTo(Date.from(Instant.now()).getTime());
        });
    }

    @Test
    void is_Expired_Token_test() {
        String jwtGenerated = tokenManager.generateToken(user.email(), user.userId().toString(), user.roles());
        Optional<Claims> claims = tokenManager.extractClaims(jwtGenerated);
        claims.ifPresent(claim -> {

            long exp = claim.getExpiration().getTime();
            assertThat(exp).isGreaterThanOrEqualTo(Date.from(Instant.now().minusMillis(accessTokenExpiration)).getTime());
        });
    }

    @Test
    void validate_Token_test() {
        String jwtGenerated = tokenManager.generateToken(user.email(), user.userId().toString(), user.roles());
        assertThat(tokenManager.validateToken(jwtGenerated)).isTrue();
    }

    @Test
    void generate_Refresh_Token_test() {
        String jwtGenerated = tokenManager.generateToken(user.email(), user.userId().toString(), user.roles());
        Optional<Claims> claims = tokenManager.extractClaims(jwtGenerated);
        claims.ifPresent(claim -> {
            String email = claim.getSubject();
            // je pense que si le token est rafrachis donc, il est reénouvellé, donc pas identique au premier qui a été généré
            assertThat(tokenManager.generateRefreshToken(email)).isNotEqualTo(jwtGenerated);
        });
    }

    @Test
    void has_one_Authorities_test() {

        String jwtGenerated = tokenManager.generateToken(user.email(), user.userId().toString(), user.roles());
        List<String> authorities = new ArrayList<>(user.roles());
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        grantedAuthorities.add(new SimpleGrantedAuthority(authorities.getFirst()));
        assertThat(authorities).isNotNull();
        assertThat(authorities).hasSize(3);
        assertThat(grantedAuthorities.getFirst().getAuthority()).isEqualTo("ROLE_STUDENT");
    }

    @Test
    void is_RefreshToken_test() {
        String jwtGenerated = tokenManager.generateToken(user.email(), user.userId().toString(), user.roles());

        // Pas de claim "type"
        boolean accessToken = tokenManager.isRefreshToken(jwtGenerated);
        // donc jeton access crée
        assertThat(accessToken).isFalse();

        String refreshToken = tokenManager.generateRefreshToken(user.email());
        // Claim "type" ="refresh"
        assertThat(tokenManager.isRefreshToken(refreshToken)).isTrue();
    }
}