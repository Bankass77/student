package com.service.webfluxcourse.service;

import com.service.webfluxcourse.dto.AuthResponse;
import com.service.webfluxcourse.dto.LoginRequest;
import com.service.webfluxcourse.dto.SignupRequest;
import reactor.core.publisher.Mono;

public interface AuthService {

    Mono<AuthResponse> signup(SignupRequest signupRequest);
    Mono<AuthResponse> login(LoginRequest loginRequest);
}
