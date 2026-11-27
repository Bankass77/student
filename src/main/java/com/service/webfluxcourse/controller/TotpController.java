package com.service.webfluxcourse.controller;

import com.service.webfluxcourse.dto.AuthResponse;
import com.service.webfluxcourse.dto.TotpVerifyRequest;
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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/totp")
public class TotpController {

    private final TotpService totpService;
    private final AuthService authService;

    public TotpController(TotpService totpService, AuthService authService) {
        this.totpService = totpService;
        this.authService = authService;
    }


    // Générer le secret TOTP et le QR code pour un utilisateur connecté
    @PostMapping("/setup")
    public Mono<ResponseEntity<Map<String, String>>> setupTotp(Authentication authentication) {

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        String userId = jwt.getClaimAsString("userId");
        String email = jwt.getClaimAsString("sub");
        return authService.setupTotp(userId).map(
                secret -> {
                    String qrCodeUri = totpService.generateQrCodeUri(secret, email, "MonApp");
                    Map<String, String> response = new HashMap<>();
                    response.put("qrCodeUri", qrCodeUri);
                    response.put("secret", secret);
                    return ResponseEntity.ok(response);

                }
        );
    }


    // Activer le TOTP après vérification du premier code
    @PostMapping("/activate")
    public Mono<ResponseEntity<Void>> activateTotp(Authentication authentication, @RequestBody @Valid TotpVerifyRequest request) {
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Jwt jwt)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String userId = jwt.getClaimAsString("userId");
        return authService.activeTotp(userId, request.code()).then(Mono.just(ResponseEntity.ok().<Void>build()));
    }

    // Vérifier le code TOTP lors du login (étape 2)
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> loginTotp(@RequestHeader("Authorization") String authorizationHeader,
                                                        @RequestBody @Valid TotpVerifyRequest request) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Mono.just((ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
        }
        String tempToken = authorizationHeader.substring(7); // enlever "Bearer"
        return authService.loginStep2Totp(tempToken, request.code()).map(authResponse -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON).body(authResponse));
    }

}
