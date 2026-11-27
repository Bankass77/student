package com.service.webfluxcourse.security;

import com.service.webfluxcourse.domain.User;
import com.service.webfluxcourse.dto.AuthResponse;
import com.service.webfluxcourse.dto.LoginRequest;
import com.service.webfluxcourse.dto.SignupRequest;
import com.service.webfluxcourse.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SecurityConfigTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        String password = BCrypt.hashpw("MonPass123!", BCrypt.gensalt());
        String email = "fatoumata@seznam.cz";
        User user = new User(UUID.randomUUID(), email, password, "Fatoumata", new String[]{"ROLE_USER"},
                true, false, true, Instant.now(), null, null, false);
        userRepository.save(user).block();
    }

    @Test
    public void login_shouldReturnToken_whenCredentialsAreValid() {
        // Prérequis : un utilisateur existe déjà en base (ou crée-le avant)
        LoginRequest loginRequest = new LoginRequest("fatoumata@seznam.cz", "MonPass123!");
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(authResponse -> {
                    assertThat(authResponse.token()).isNotEmpty();
                    assertThat(authResponse.tokenType()).isEqualTo("Bearer");
                });
    }

    @Test
    void signup_shouldCreateUserAndReturnToken() {
        String uniqueEmail = "newUser"+ System.currentTimeMillis()+ "@example.com";
        SignupRequest signupRequest = new SignupRequest(
                uniqueEmail, "MonPass123!", "newUser"
        );
        webTestClient.post()
                .uri("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(signupRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.token").isNotEmpty();
    }

    @Test
    void me_should_be_return_401_without_token() {

        webTestClient.get()
                .uri("/api/v1/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}