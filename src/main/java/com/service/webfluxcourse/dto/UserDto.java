package com.service.webfluxcourse.dto;


import com.service.webfluxcourse.domain.User;

import java.util.Set;

public record UserDto(String userId, String email, Set<String> roles) {
    public static UserDto from(User user) {
        return new UserDto(user.userId().toString(), user.email(), user.roles());
    }
}
