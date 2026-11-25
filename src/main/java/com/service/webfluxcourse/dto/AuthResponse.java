package com.service.webfluxcourse.dto;

public record AuthResponse(String userId, String email, String token, String tokenType, long expiresIn) {
}
