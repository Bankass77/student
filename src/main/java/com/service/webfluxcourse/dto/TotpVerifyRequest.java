package com.service.webfluxcourse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TotpVerifyRequest(
        @NotBlank(message = "Le code TOTP est requis") @Size(min = 6, max = 6, message = "Le code TOTP doit contenir exactement 6 chiffres") String code) {
}
