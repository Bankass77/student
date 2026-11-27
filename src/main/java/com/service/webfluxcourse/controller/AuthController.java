package com.service.webfluxcourse.controller;

import com.service.webfluxcourse.dto.*;
import com.service.webfluxcourse.service.AuthService;
import com.service.webfluxcourse.service.TotpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;
    private  final TotpService  totpService;

    public AuthController(AuthService authService, TotpService totpService) {
        this.authService = authService;
        this.totpService = totpService;
    }

    @PostMapping("/auth/signup")
    public Mono<ResponseEntity<AuthResponse>> signup(@RequestBody @Valid SignupRequest signupRequest) {

        return authService.signup(signupRequest).map(authResponse -> ResponseEntity
                .created(URI.create("/auth/signup"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(authResponse));
    }

    @PostMapping("/auth/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody @Valid LoginRequest loginRequest) {

        return authService.login(loginRequest).map(authResponse -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON).body(authResponse));


    }

/*    @GetMapping("/me")
    public  Mono<ResponseEntity<UserDto>> getCurrentUser( @AuthenticationPrincipal User user) {

        return Mono.just(ResponseEntity.ok(UserDto.from(user)));
    }*/

    // ou avec Authentication
    @GetMapping("/auth/me")
    public Mono<ResponseEntity<?>> getCurrentUser(Authentication authentication) {

        // authentication.getPrincipal() contient l'Utilisateur authentifé
        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {

            String userId = jwt.getClaimAsString("userId") !=null ? jwt.getClaimAsString("userId") : jwt.getSubject();
            String email = jwt.getClaimAsString("sub");
            // Gérer l'absence du claim "roles"
            List<String> rolesFromClaim = jwt.getClaimAsStringList("roles");
            Set<String> roles = rolesFromClaim == null
                    ? new HashSet<>()
                    : new HashSet<>(rolesFromClaim);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("email", email);
            userInfo.put("roles", roles);
            userInfo.put("userId", userId);
            return Mono.just(ResponseEntity.ok(userInfo));
        }

        // cas de sécurité : on ne sait pâs qui c'est
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

}
