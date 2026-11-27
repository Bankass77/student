package com.service.webfluxcourse.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("users")
public record User(@Id UUID userId, @Email String email, @Min(message = "le mot de doit contenir au moins 8 caractètes",
        value = 8) @Max(message = "le max du mot de pass est de 12 caratères", value = 12) String password,
                   String username, String[] roles, boolean active,
                   boolean locked, boolean enabled,
                   Instant createdAt, Instant lastLoginAt, @Min(value = 6) @Max(value = 6) String totpSecret,
                   boolean totpEnabled) {
}
