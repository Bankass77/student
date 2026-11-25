package com.service.webfluxcourse.service;

import com.service.webfluxcourse.dto.AuthResponse;
import com.service.webfluxcourse.dto.LoginRequest;
import com.service.webfluxcourse.dto.SignupRequest;
import reactor.core.publisher.Mono;

public interface AuthService {

    Mono<AuthResponse> signup(SignupRequest signupRequest);

    Mono<AuthResponse> login(LoginRequest loginRequest);

    // Génère le secret Totp pour un utilisateur existant
    Mono<String> setupTotp(String userId);

    // Active le TOTP après vérification du premier code
    Mono<Void> activeTotp(String userdId, String code);

    // Login étape 1 : vérifie le mot de passe, émet un token temporaire "MFA en attente"
    Mono<String> loginStep1Password(LoginRequest loginRequest);

    // Login étape 2 : vérifie le code TOTP, émet le token final
    Mono<AuthResponse> loginStep2Totp(String tempToken, String code);

}
