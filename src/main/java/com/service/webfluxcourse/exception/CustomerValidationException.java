package com.service.webfluxcourse.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class CustomerValidationException extends RuntimeException {
    private final List<?> errors;

    public CustomerValidationException(List<?> errors) {
        super("Customer validation failed");
        this.errors = errors;
    }

}
