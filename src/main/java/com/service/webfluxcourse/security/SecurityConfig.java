package com.service.webfluxcourse.security;

import com.service.webfluxcourse.config.WebFluxCorsConfiguration;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final TokenManager tokenManager;

    public SecurityConfig(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        return http.
                cors(Customizer.withDefaults()).
                csrf(ServerHttpSecurity.CsrfSpec::disable)  // JWT est stateless, pas besoin de CSF
                .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)).accessDeniedHandler(
                        new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN)
                ))
                .authorizeExchange(exchanges ->
                        // Routes public(inscription, connexion)
                        exchanges.pathMatchers(HttpMethod.POST, "/api/v1/auth/signup", "/api/v1/auth/login").permitAll()
                                .pathMatchers("/actuator/**").permitAll()
                                //Tout le reste nécessite d'être connecter
                                .anyExchange().authenticated())
                .oauth2ResourceServer(oAuth2 -> oAuth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))

                .build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        // On recrée la même clé que dans TokenManager, à partir du secret.
        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // on mappe le claim "sub" vers le principal
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Configuration des rôles (authorities)
            List<String> rolesFromClaim= jwt.getClaimAsStringList("roles");

            Set<String> roles = rolesFromClaim == null ? new HashSet<>(): new HashSet<>(rolesFromClaim);
            return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
        });
        // Adaptation synchrone -> reactive
        return jwt -> Mono.just(converter.convert(jwt));
    }
}
