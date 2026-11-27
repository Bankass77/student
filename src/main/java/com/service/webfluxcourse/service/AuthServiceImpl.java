package com.service.webfluxcourse.service;

import com.service.webfluxcourse.domain.User;
import com.service.webfluxcourse.dto.AuthResponse;
import com.service.webfluxcourse.dto.LoginRequest;
import com.service.webfluxcourse.dto.SignupRequest;
import com.service.webfluxcourse.exception.InvalidCredentialsException;
import com.service.webfluxcourse.exception.UserExistsException;
import com.service.webfluxcourse.exception.UserNotFoundException;
import com.service.webfluxcourse.repository.UserRepository;
import com.service.webfluxcourse.security.TokenManager;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TokenManager tokenManager;
    private final TotpService totpService;

    public AuthServiceImpl(UserRepository userRepository, TokenManager tokenManager, TotpService totpService) {

        this.userRepository = userRepository;
        this.tokenManager = tokenManager;
        this.totpService = totpService;
    }

    @Override
    public Mono<AuthResponse> signup(SignupRequest signupRequest) {

        return userRepository.existsByEmail(signupRequest.email()).flatMap(
                exists -> {
                    if (exists) {

                        return Mono.error(new UserExistsException("Email already exists"));
                    }
                    String password = BCrypt.hashpw(signupRequest.password(), BCrypt.gensalt());
                    String[] roles = new String[]{"ROLE_USER"};

                    User newUser = new User(null, signupRequest.email(), password, signupRequest.username(), roles,
                            true, false, true, Instant.now(), null, null, false);
                    return userRepository.save(newUser);
                }).map(
                savedUser -> {
                    String token = tokenManager.generateToken(savedUser.userId().toString(), savedUser.email(), savedUser.roles());
                    long exp = tokenManager.extractExpiration(token).getTime();
                    return new AuthResponse(savedUser.userId().toString(), savedUser.email(), token, "Bearer", exp);
                });

    }

    @Override
    public Mono<AuthResponse> login(LoginRequest loginRequest) {

        return userRepository.findByEmail(loginRequest.email()).switchIfEmpty(
                Mono.error(new InvalidCredentialsException("Invalid email or password"))).flatMap(
                user -> {
                    if (!BCrypt.checkpw(loginRequest.password(), user.password())) {
                        throw new InvalidCredentialsException("Invalid email or password");
                    }
                    String generateToken = tokenManager.generateToken(user.userId().toString(), user.email(), user.roles());
                    long exp = tokenManager.extractExpiration(generateToken).getTime();
                    String email = tokenManager.extractEmail(generateToken);
                    String userId = tokenManager.extractUserId(generateToken);
                    return Mono.just(new AuthResponse(userId, email, generateToken, "Bearer", exp));
                });
    }

    // ========================================================================
    // MFA - ÉTAPE 1 : Générer le secret TOTP après inscription
    // ========================================================================
    @Override
    public Mono<String> setupTotp(String userId) {

        UUID uuid = UUID.fromString(userId);

        return userRepository.findById(uuid).switchIfEmpty(Mono.error(new UserNotFoundException("Utilisateur introuvable"))).flatMap(
                user -> {
                    String secret = totpService.generateSecret();
                    return totpService.saveTotpSecret(uuid, secret).thenReturn(secret);
                }
        );
    }


    // ========================================================================
    // MFA - ÉTAPE 2 : Activer le TOTP après vérification du premier code
    // ========================================================================
    @Override
    public Mono<Void> activeTotp(String userdId, String code) {

        UUID uuid = UUID.fromString(userdId);

        return userRepository.findById(uuid).switchIfEmpty(Mono.error(new UserNotFoundException("Utilisateur introuvable"))).flatMap(
                user -> {
                    if (user.totpSecret() == null) {
                        return Mono.error(new InvalidCredentialsException("Aucun secret TOTP configuré"));
                    }

                    boolean valid = totpService.verifyCode(user.totpSecret(), code);
                    if (!valid) {
                        return Mono.error(new InvalidCredentialsException("Aucun secret TOTP valid"));
                    }
                    return totpService.enableTotp(uuid).then();
                }
        );
    }

    // ========================================================================
    // MFA - LOGIN ÉTAPE 1 : Mot de passe → token temporaire
    // ========================================================================
    @Override
    public Mono<String> loginStep1Password(LoginRequest loginRequest) {
        return userRepository.findByEmail(loginRequest.email()).switchIfEmpty(Mono.error(new UserNotFoundException("Aucun utilisateur trouvé avec cet email"))).flatMap(
                user -> {
                    if (!BCrypt.checkpw(loginRequest.password(), user.password())) {
                        return Mono.error(new InvalidCredentialsException("Invalid email or password"));
                    }

                    // Token temporaire avec le claim mfa_pending = true
                    String tempToken = tokenManager.generateTempMFAToken(user.email(), user.userId().toString());
                    return Mono.just(tempToken);
                }
        );
    }

    // ========================================================================
    // MFA - LOGIN ÉTAPE 2 : Code TOTP → token final
    // ========================================================================
    @Override
    public Mono<AuthResponse> loginStep2Totp(String tempToken, String code) {

        // Valider le token temporaire (vérifier signature + expiration + claim mfa_pending)

        if (!tokenManager.validateToken(tempToken)) {
            return Mono.error(new InvalidCredentialsException("Invalid token tempo ou expiré"));
        }

        String email = tokenManager.extractEmail(tempToken);
        String userId = tokenManager.extractUserId(tempToken);
        // Vérifier que le token temporaire a bien le claim mfa_pending
        // (optionnel mais recommandé pour la sécurité)
        return userRepository.findById(UUID.fromString(userId)).switchIfEmpty(Mono.error(new UserNotFoundException("Utilisateur introuvable"))).flatMap(
                user -> {

                    if (!user.totpEnabled()) {
                        return Mono.error(new InvalidCredentialsException("Le TOTP n'est pas activé pour ce compte"));
                    }

                    boolean valid = totpService.verifyCode(user.totpSecret(), code);

                    if (!valid) {
                        return Mono.error(new InvalidCredentialsException("Aucun  code TOTP valid"));
                    }

                    // On emet le token final
                    String finalToken = tokenManager.generateToken(user.userId().toString(), user.email(), user.roles());
                    long exp = tokenManager.extractExpiration(finalToken).getTime();
                    return Mono.just(new AuthResponse(userId, email, finalToken, "Bearer", exp));
                }
        );
    }

}
