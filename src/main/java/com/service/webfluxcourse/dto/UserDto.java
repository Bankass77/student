package com.service.webfluxcourse.dto;


import com.service.webfluxcourse.domain.User;
import jakarta.validation.constraints.Email;

import java.util.Set;

public record UserDto(String userId, @Email(message = "Email est invalide ") String email, String[] roles) {
    public static UserDto from(User user) {
        return new UserDto(user.userId().toString(), user.email(), user.roles());
    }
}
