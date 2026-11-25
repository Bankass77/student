package com.service.webfluxcourse.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Table("users")
public record User(@Id UUID userId, String email, String password,
                   String username, Set<String> roles, boolean active,
                   boolean locked, boolean enabled,
                   Instant createdAt, Instant lastLoginAt) {
}
