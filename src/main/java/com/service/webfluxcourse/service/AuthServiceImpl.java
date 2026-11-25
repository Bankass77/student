package com.service.webfluxcourse.service;

import com.service.webfluxcourse.domain.User;
import com.service.webfluxcourse.dto.AuthResponse;
import com.service.webfluxcourse.dto.LoginRequest;
import com.service.webfluxcourse.dto.SignupRequest;
import com.service.webfluxcourse.exception.InvalidCredentialsException;
import com.service.webfluxcourse.exception.UserExistsException;
import com.service.webfluxcourse.repository.UserRepository;
import com.service.webfluxcourse.security.TokenManager;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TokenManager tokenManager;

    public AuthServiceImpl(UserRepository userRepository, TokenManager tokenManager) {

        this.userRepository = userRepository;
        this.tokenManager = tokenManager;
    }

    @Override
    public Mono<AuthResponse> signup(SignupRequest signupRequest) {

        return userRepository.existsByEmail(signupRequest.email()).flatMap(
                exists -> {
                    if (exists) {

                        return Mono.error(new UserExistsException("Email already exists"));
                    }
                    String password = BCrypt.hashpw(signupRequest.password(), BCrypt.gensalt());
                    Set<String> roles = Set.of("ROLE_USER");

                    User newUser = new User(null, signupRequest.email(), password, signupRequest.username(), roles,
                            true, false, true, Instant.now(), null);
                    return userRepository.save(newUser);
                }).map(
                savedUser -> {
                    String token = tokenManager.generateToken(savedUser.email(), savedUser.userId().toString(), savedUser.roles());
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
                    String generateToken = tokenManager.generateToken(user.email(), user.userId().toString(), user.roles());
                    long exp = tokenManager.extractExpiration(generateToken).getTime();
                    String email = tokenManager.extractEmail(generateToken);
                    String userId = tokenManager.extractUserId(generateToken);
                    return Mono.just(new AuthResponse(userId, email, generateToken, "Bearer", exp));
                });
    }
}
