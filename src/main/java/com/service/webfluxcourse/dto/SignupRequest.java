package com.service.webfluxcourse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(@NotBlank(message = "Username is required") @Email(message = "invalid email format") String email,
                            @NotBlank(message = "Password is required")  @Size(min = 8, message = "Password must be at least 8 characters") String password,
                            @NotBlank(message = "User Name is required") String username) {
}
